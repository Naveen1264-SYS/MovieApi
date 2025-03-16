package com.movieflix.movieApi.service;

import com.movieflix.movieApi.dto.MovieDto;
import com.movieflix.movieApi.dto.MoviePageResponse;
import com.movieflix.movieApi.entities.Movie;
import com.movieflix.movieApi.exceptions.MovieNotFoundException;
import com.movieflix.movieApi.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieServiceImpl implements MovieService {


    @Autowired
    private final MovieRepository movieRepository;

    private final FileService fileService;

    @Value("${project.poster}")
    private String path;

    @Value("${project.base.url}")
    private String baseUrl;

    public MovieServiceImpl(MovieRepository movieRepository, FileService fileService) {
        this.movieRepository = movieRepository;
        this.fileService = fileService;
    }


    @Override
    public MovieDto addMovie(MovieDto movieDto, MultipartFile file) throws IOException {

        // 1.Upload The file
        if (Files.exists(Paths.get(path + File.separator + file.getOriginalFilename()))) {

            throw new FileAlreadyExistsException("File already exists ! Please enter another File Name");
        }
        String uploadedFileName = fileService.uploadFile(path, file);

        //2. To set the value of failed 'poster'
        movieDto.setPoster(uploadedFileName);

        // 3. map dto  to Movie object
        Movie movie = new Movie(
                null,
                movieDto.getTitle(),
                movieDto.getDirector(),
                movieDto.getStudio(),
                movieDto.getMovieCast(),
                movieDto.getReleaseYear(),
                movieDto.getPoster()
        );

        //4. save the movie object -> saved Movie  object

        Movie savedMovie = movieRepository.save(movie);

        //5. Generate the posterUrl

        String posterUrl = baseUrl + "/file/" + uploadedFileName;

        //6.map Movie object to DTO object and return it
        MovieDto response = new MovieDto(
                savedMovie.getMovieId(),
                savedMovie.getTitle(),
                savedMovie.getDirector(),
                savedMovie.getStudio(),
                savedMovie.getMovieCast(),
                savedMovie.getReleaseYear(),
                savedMovie.getPoster(),
                posterUrl
        );

        return response;
    }

    @Override
    public MovieDto getMovie(Integer movieId) {
        // 1.To Check data in Db and if exists, fetch the data of given ID
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie Not Found with id = " + movieId));

        //2.Generate PosterUrl
        String posterUrl = baseUrl + "/file/" + movie.getPoster();


        // 3. Map to MovieDto Object and return it
        MovieDto response = new MovieDto(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getStudio(),
                movie.getMovieCast(),
                movie.getReleaseYear(),
                movie.getPoster(),
                posterUrl

        );

        return response;
    }

    @Override
    public List<MovieDto> getAllMovies() {
        //1. fetch all data from DB
        List<Movie> movies = movieRepository.findAll();

        List<MovieDto> movieDtos = new ArrayList<>();

        //2. iterate through the list ,generate posterUrl for each movie obj
        // and map to MovieDto  obj
        for (Movie movie : movies) {
            String posterUrl = baseUrl + "/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl

            );
            movieDtos.add(movieDto);
        }
        return movieDtos;
    }

    @Override
    public MovieDto updateMovie(Integer movieId, MovieDto movieDto, MultipartFile file) throws IOException {
        //1. check if movie object exists with given movieId
        Movie mv = movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie Not Found with id = " + movieId));


        // 2. If file is null , do nothing
        // if file is not null, then delete existing file  associated with the  record and,
        //and upload the new file
        String fileName = mv.getPoster();
        if (file != null) {
            Files.delete(Paths.get(path + File.separator + fileName));
            fileName = fileService.uploadFile(path, file);
        }

        //3. set movieDto's poster value , according to step 2
        movieDto.setPoster(fileName);

        //4. map it to movie object
        Movie movie = new Movie(
                mv.getMovieId(),
                movieDto.getTitle(),
                movieDto.getDirector(),
                movieDto.getStudio(),
                movieDto.getMovieCast(),
                movieDto.getReleaseYear(),
                movieDto.getPoster()

        );

        //5. save the movie Object -> return saved movie object
        Movie updatedMovie = movieRepository.save(movie);

        // 6. Generate poster for it
        String posterUrl = baseUrl + "/file/" + fileName;


        // 7.map to MovieDto and return
        MovieDto response = new MovieDto(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getStudio(),
                movie.getMovieCast(),
                movie.getReleaseYear(),
                movie.getPoster(),
                posterUrl

        );

        return response;
    }

    @Override
    public String deleteMovie(Integer movieId) throws IOException {
        //1. check if movie object exists in Db
        Movie mv = movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie Not Found with id = " + movieId));
        Integer id = mv.getMovieId();
        //2. delete the file associates with this object

        Files.deleteIfExists(Paths.get(path + File.separator + mv.getPoster()));
        //3. delete the movie object

        movieRepository.delete(mv);
        return "Movie deleted with id = " + id;
    }

    @Override
    public MoviePageResponse getAllMoviesWithPagination(Integer pageNumber, Integer pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);


        Page<Movie> moviePages = movieRepository.findAll(pageable);


        List<Movie> movies = moviePages.getContent();

        List<MovieDto> movieDtos = new ArrayList<>();

        for (Movie movie : movies) {
            String posterUrl = baseUrl + "/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl

            );
            movieDtos.add(movieDto);
        }


        return new MoviePageResponse(movieDtos, pageNumber, pageSize,
                moviePages.getTotalElements(),
                moviePages.getTotalPages(),
                moviePages.isLast()
        );
    }

    @Override
    public MoviePageResponse getAllMoviesWithPaginationAndSorting(Integer pageNumber, Integer pageSize, String sortBy, String dir) {
        Sort sort = dir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);


        Page<Movie> moviePages = movieRepository.findAll(pageable);


        List<Movie> movies = moviePages.getContent();

        List<MovieDto> movieDtos = new ArrayList<>();

        for (Movie movie : movies) {
            String posterUrl = baseUrl + "/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl

            );
            movieDtos.add(movieDto);
        }


        return new MoviePageResponse(movieDtos, pageNumber, pageSize,
                moviePages.getTotalElements(),
                moviePages.getTotalPages(),
                moviePages.isLast());


    }
}
