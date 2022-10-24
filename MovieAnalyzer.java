import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * java 2 assignment-1
 * Public Class
 *
 * @author 12110919
 */

public class MovieAnalyzer {
    private final List<Movie> movies;
    private final Set<String> genres = new HashSet<>(Arrays.asList(
            "Drama", "Crime", "Comedy", "Adventure", "Action",
            "Romance", "Thriller", "Biography", "Mystery",
            "Animation", "Sci-Fi", "War", "Fantasy", "History", "Family",
            "Music", "Film-Noir", "Horror", "Sport", "Western", "Musical")
    );

    public MovieAnalyzer(String path) {
        movies = new ArrayList<>();
        String line = "";
        try (BufferedReader br = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            line = br.readLine();
            int cnt = 0;
            while ((line = br.readLine()) != null) {
                String[] columns = line.trim().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                movies.add(new Movie(++cnt,
                                formString(columns[1]), columns[5].split(","), formString(columns[7]), formString(columns[3]), formString(columns[9]),
                                new String[]{columns[10], columns[11], columns[12], columns[13]},
                                Integer.parseInt(columns[2]),
                                Integer.parseInt(columns[4].substring(0, columns[4].indexOf("min")).trim()),
                                Float.parseFloat(columns[6]),
                                columns[8].length() == 0 ? null : Integer.parseInt(columns[8]),
                                Integer.parseInt(columns[14]),
                                formInt(columns[15])
                        )
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(line);
        }
    }

    public Map<Integer, Integer> getMovieCountByYear() {
        return new TreeMap<>(
                movies.stream().collect(
                        Collectors.groupingBy(
                                Movie::getYear,
                                Collectors.summingInt(e -> 1)
                        )
                )
        ).descendingMap();
    }

    public Map<String, Integer> getMovieCountByGenre() {
        Map<String, Integer> res = movies.stream().flatMap(w -> {
            ArrayList<String> t = new ArrayList<>();
            List<String> str = Arrays.asList(w.getGenre());
            System.out.println(str);
            genres.forEach(s -> {
                if (str.contains(s)) {
                    t.add(s);
                }
            });
            return t.stream();
        }).collect(
                Collectors.groupingBy(
                        String::valueOf,
                        Collectors.summingInt(e -> 1))
        );
        ArrayList<Map.Entry<String, Integer>> entryList = new ArrayList<>(res.entrySet());
        entryList.sort((o1, o2) -> {
            int val = o2.getValue() - o1.getValue();
            return val == 0 ? o1.getKey().compareTo(o2.getKey()) : val;
        });
        return entryList.stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e, n) -> {
                    throw new IllegalStateException();
                },
                LinkedHashMap::new));
    }


    public Map<List<String>, Integer> getCoStarCount() {
        Map<ToString, Integer> t = new TreeMap<>(
                (o1, o2) -> !o1.getS().get(0).equals(o2.getS().get(0))
                        ? o1.getS().get(0).compareTo(o2.getS().get(0))
                        : o1.getS().get(1).compareTo(o2.getS().get(1))
        );
        movies.forEach(m -> {
            String[] stars = m.getStars();
            for (int i = 0; i < stars.length; i++) {
                for (int j = i + 1; j < stars.length; j++) {
                    ToString ts = new ToString(Arrays.asList(stars[i], stars[j]));
                    if (!t.containsKey(ts)) {
                        t.put(ts, 0);
                    }
                    t.put(ts, t.get(ts) + 1);
                }
            }
        });
        ArrayList<Map.Entry<ToString, Integer>> entryList = new ArrayList<>(t.entrySet());
        return entryList.stream().collect(Collectors.toMap(
                tt -> tt.getKey().getS(),
                Map.Entry::getValue,
                (e, n) -> {
                    throw new IllegalStateException();
                }, LinkedHashMap::new)
        );
    }

    public List<String> getTopMovies(int k, String by) {
        if ("runtime".equals(by)) {
            return movies.stream().
                    sorted((o1, o2) -> {
                        int t = o2.getRuntime() - o1.getRuntime();
                        return t == 0 ? o1.getName().compareTo(o2.getName()) : t;
                    })
                    .map(Movie::getName).limit(k).collect(Collectors.toList());
        } else {
            return movies.stream()
                    .sorted((o1, o2) -> {
                        int t = o2.getOverview().length() - o1.getOverview().length();
                        return t == 0 ? o1.getName().compareTo(o2.getName()) : t;
                    })
                    .map(Movie::getName).limit(k).collect(Collectors.toList());
        }
    }

    public List<String> getTopStars(int k, String by) {
        Map<String, Set<Movie>> map = new TreeMap<>();
        Comparator<Map.Entry<String, Set<Movie>>> c1 = (o1, o2) -> {
            double t = o2.getValue().stream().mapToDouble(Movie::getRanting).average().getAsDouble() -
                    o1.getValue().stream().mapToDouble(Movie::getRanting).average().getAsDouble();
            return t == 0 ? o1.getKey().compareTo(o2.getKey()) : (t > 0 ? 1 : -1);
        };
        Comparator<Map.Entry<String, Set<Movie>>> c2 = (o1, o2) -> {
            double t = o2.getValue().stream().mapToLong(Movie::getGross).average().getAsDouble() -
                    o1.getValue().stream().mapToLong(Movie::getGross).average().getAsDouble();
            return t == 0 ? o1.getKey().compareTo(o2.getKey()) : (t > 0 ? 1 : -1);
        };
        for (Movie movie : movies) {
            if ("gross".equals(by) && movie.getGross() == null) {
                continue;
            }
            String[] stars = movie.getStars();
            for (String star : stars) {
                if (!map.containsKey(star)) {
                    map.put(star, new HashSet<>());
                }
                map.get(star).add(movie);
            }
        }
        ArrayList<Map.Entry<String, Set<Movie>>> entryList = new ArrayList<>(map.entrySet());
        if ("rating".equals(by)) {
            entryList.sort(c1);
        } else {
            entryList.sort(c2);
        }
        return entryList.stream().map(Map.Entry::getKey).limit(k).collect(Collectors.toList());
    }

    public List<String> searchMovies(String genre, float min_rating, int max_runtime) {
        return movies.stream()
                .filter(m -> m.getRanting() >= min_rating && m.getRuntime() <= max_runtime)
                .filter(m -> Arrays.asList(m.getGenre()).contains(genre))
                .map(Movie::getName).sorted(String::compareTo).collect(Collectors.toList());
    }


    String formString(String name) {
        if (name.matches("^\".*\"$")) {
            name = name.substring(1, name.length() - 1);
        }
        return name;
    }

    Long formInt(String data) {
        if (data.trim().length() == 0) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < data.length(); i++) {
            if (Character.isDigit(data.charAt(i))) {
                b.append(data.charAt(i));
            }
        }
        return Long.parseLong(b.toString());
    }

    public static void main(String[] args) {
        MovieAnalyzer m = new MovieAnalyzer("resources/imdb_top_500.csv");
        m.getCoStarCount().forEach((k, v) -> {
            System.out.println(k + " == " + v);
        });
    }
}

class ToString {
    private List<String> s;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ToString toString = (ToString) o;
        return Objects.equals(s, toString.s);
    }

    @Override
    public int hashCode() {
        return Objects.hash(s);
    }

    public ToString(List<String> s) {
        s.sort(String::compareTo);
        this.s = s;
    }

    public List<String> getS() {
        return s;
    }

    public void setS(List<String> s) {
        this.s = s;
    }
}

@SuppressWarnings("all")
class Movie {
    private int id;
    private String name;
    private String[] genre;
    private String overview;
    private String certificate;
    private String director;
    private String[] stars;
    private int year;
    private int runtime;
    private float ranting;
    private Integer score;
    private int voteCount;
    private Long gross;

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Movie movie = (Movie) o;
        return id == movie.id;
    }

    public String[] getGenre() {
        return genre;
    }


    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getName() {
        return name;
    }


    public String getOverview() {
        return overview;
    }


    public String[] getStars() {
        return stars;
    }

    public int getYear() {
        return year;
    }


    public int getRuntime() {
        return runtime;
    }

    public double getRanting() {
        return ranting;
    }

    public Long getGross() {
        return gross;
    }


    public Movie(int id, String name, String[] genres, String overview, String certificate, String director, String[] stars, int year, int runtime, float ranting, Integer score, int voteCount, Long gross) {
        for (int i = 0; i < genres.length; i++) {
            if (genres[i].charAt(0) == '"') {
                genres[i] = genres[i].substring(1);
            } else if (genres[i].charAt(genres[i].length() - 1) == '"') {
                genres[i] = genres[i].substring(0, genres[i].length() - 1);
            }
            genres[i] = genres[i].trim();
        }
        this.id = id;
        this.name = name;
        this.genre = genres;
        this.overview = overview;
        this.certificate = certificate;
        this.director = director;
        this.stars = stars;
        this.year = year;
        this.runtime = runtime;
        this.ranting = ranting;
        this.score = score;
        this.voteCount = voteCount;
        this.gross = gross;
    }
}