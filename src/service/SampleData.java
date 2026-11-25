package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import model.Gender;
import model.GenderPreference;
import model.HouseholdPreferences;
import model.Listing;
import model.Message;
import model.Neighborhoods;
import model.PermanentListing;
import model.RoomType;
import model.SleepSchedule;
import model.TemporaryListing;
import model.User;

/**
 * Demo accounts and listings. All demo passwords are "password123".
 *
 * <p>Besides a few hand-written listings, about two hundred more are generated from the
 * neighborhood profiles below with a fixed random seed, so every install gets the same data.
 * Photos ship with the app under {@code /images/sample} (see {@code docs/PHOTO_CREDITS.md}).
 */
final class SampleData {
    /** Bump this when the demo data changes; older data files are upgraded on load. */
    static final int VERSION = 2;
    static final String DEMO_PASSWORD = "password123";
    private static final String DEMO_DOMAIN = "@demo.com";
    private static final int GENERATED_LISTINGS = 196;

    private SampleData() {
    }

    // ---------------------------------------------------------------- neighborhood profiles

    /** What a neighborhood is like: typical private-room rent, and phrases for titles and descriptions. */
    private record Area(String name, String slug, int minRent, int maxRent, String landmark, String transit,
            String nearby, String... housing) {
    }

    private static final List<Area> AREAS = List.of(
            new Area("Back Bay", "back-bay", 1500, 2100, "Copley Square", "the Green Line at Copley",
                    "Newbury Street, the Commonwealth Avenue Mall and the Charles River Esplanade", "brownstone", "row house"),
            new Area("Beacon Hill", "beacon-hill", 1450, 2000, "Charles Street", "the Red Line at Charles/MGH",
                    "the gas-lit streets, Charles Street cafés and the Public Garden", "brick row house", "walk-up"),
            new Area("South End", "south-end", 1400, 1950, "the SoWa market", "the Orange Line at Back Bay",
                    "Tremont Street restaurants, little parks and the SoWa market", "Victorian row house", "brownstone"),
            new Area("North End", "north-end", 1300, 1800, "Hanover Street", "Haymarket on the Orange and Green Lines",
                    "Hanover Street bakeries, the Greenway and the harbor walk", "walk-up", "brick apartment"),
            new Area("Fenway", "fenway", 1250, 1750, "Fenway Park", "the Green Line at Kenmore",
                    "Fenway Park, the Back Bay Fens and the Northeastern and BU campuses", "apartment", "brownstone"),
            new Area("Mission Hill", "mission-hill", 950, 1350, "Brigham Circle", "the E line at Brigham Circle",
                    "the Longwood hospitals, Northeastern and the Mission Hill playground", "triple-decker", "two-family house"),
            new Area("Jamaica Plain", "jamaica-plain", 1000, 1450, "Jamaica Pond", "the Orange Line at Green Street",
                    "Jamaica Pond, the Arnold Arboretum and Centre Street", "triple-decker", "Victorian house"),
            new Area("Allston", "allston", 850, 1250, "Harvard Avenue", "the B line on Comm Ave",
                    "Harvard Avenue's music venues and cheap eats", "triple-decker", "house"),
            new Area("Brighton", "brighton", 900, 1300, "the Chestnut Hill Reservoir", "the B line on Comm Ave",
                    "Brighton Center, Cleveland Circle and the reservoir loop", "two-family house", "apartment"),
            new Area("Charlestown", "charlestown", 1300, 1800, "the Navy Yard", "the Orange Line at Community College",
                    "the Navy Yard, Bunker Hill and the Harborwalk", "brick row house", "condo"),
            new Area("South Boston", "south-boston", 1250, 1750, "Castle Island", "the Red Line at Broadway",
                    "Castle Island, the beaches and the bars on East Broadway", "triple-decker", "condo"),
            new Area("Seaport", "seaport", 1700, 2400, "the Harborwalk", "the Silver Line at Courthouse",
                    "waterfront restaurants, the ICA and the Harborwalk", "high-rise apartment", "condo"),
            new Area("Dorchester", "dorchester", 800, 1200, "Savin Hill Beach", "the Red Line at Savin Hill",
                    "Savin Hill Beach, Adams Village and the Neponset trail", "triple-decker", "two-family house"),
            new Area("Roxbury", "roxbury", 800, 1150, "Franklin Park", "the Orange Line at Roxbury Crossing",
                    "Franklin Park, Nubian Square and the Southwest Corridor", "triple-decker", "row house"),
            new Area("East Boston", "east-boston", 950, 1350, "Piers Park", "the Blue Line at Maverick",
                    "Piers Park, skyline views and great taquerias", "triple-decker", "apartment"),
            new Area("Chinatown", "chinatown", 1300, 1750, "the Theater District", "the Orange Line at Chinatown",
                    "dim sum spots, the Theater District and the Common", "high-rise apartment", "walk-up"),
            new Area("West End", "west-end", 1400, 1900, "the Esplanade", "the Green Line at Science Park",
                    "TD Garden, the Museum of Science and the Esplanade", "high-rise apartment", "apartment"),
            new Area("Brookline", "brookline", 1150, 1650, "Coolidge Corner", "the C line at Coolidge Corner",
                    "tree-lined streets, the Coolidge Corner Theatre and good bakeries", "two-family house", "brick apartment"),
            new Area("Central Square", "central-square", 1200, 1700, "MIT", "the Red Line at Central",
                    "live music, food halls and MIT", "triple-decker", "apartment"),
            new Area("Harvard Square", "harvard-square", 1350, 1900, "Harvard Yard", "the Red Line at Harvard",
                    "bookstores, cafés and walks along the Charles", "Victorian house", "apartment"),
            new Area("Kendall Square", "kendall-square", 1500, 2100, "MIT", "the Red Line at Kendall/MIT",
                    "the biotech offices, MIT and the Charles River paths", "high-rise apartment", "condo"),
            new Area("Davis Square", "davis-square", 1100, 1550, "the Somerville Theatre", "the Red Line at Davis",
                    "the Somerville Theatre, the community path and lots of cafés", "triple-decker", "two-family house"),
            new Area("Union Square", "union-square", 1050, 1500, "the Union Square farmers market",
                    "the Green Line at Union Square", "the farmers market, local breweries and Bow Market",
                    "triple-decker", "two-family house"),
            new Area("Roslindale", "roslindale", 850, 1200, "Roslindale Village", "the commuter rail at Roslindale Village",
                    "the village square, the Arnold Arboretum and weekend farmers market", "two-family house", "house"));

    // ---------------------------------------------------------------- generated hosts

    private record Host(String first, String last, Gender gender, int age, String job, boolean pets, boolean smoker,
            SleepSchedule sleep) {
    }

    private static final List<Host> HOSTS = List.of(
            new Host("Maya", "Rodriguez", Gender.FEMALE, 27, "a physical therapist", true, false, SleepSchedule.EARLY_BIRD),
            new Host("Daniel", "Kim", Gender.MALE, 25, "a software engineer", false, false, SleepSchedule.NIGHT_OWL),
            new Host("Aisha", "Bello", Gender.FEMALE, 24, "a public health grad student", false, false, SleepSchedule.FLEXIBLE),
            new Host("Marcus", "Johnson", Gender.MALE, 31, "a high school teacher", true, false, SleepSchedule.EARLY_BIRD),
            new Host("Sofia", "Rossi", Gender.FEMALE, 29, "an architect", false, false, SleepSchedule.FLEXIBLE),
            new Host("Ethan", "Nguyen", Gender.MALE, 23, "an engineering student", false, false, SleepSchedule.NIGHT_OWL),
            new Host("Hannah", "Goldberg", Gender.FEMALE, 33, "a medical resident", true, false, SleepSchedule.FLEXIBLE),
            new Host("Luis", "Hernandez", Gender.MALE, 28, "a line cook", false, true, SleepSchedule.NIGHT_OWL),
            new Host("Grace", "Okafor", Gender.FEMALE, 26, "a data analyst", false, false, SleepSchedule.EARLY_BIRD),
            new Host("Ryan", "O'Connor", Gender.MALE, 30, "a nurse", true, false, SleepSchedule.FLEXIBLE),
            new Host("Mei", "Lin", Gender.FEMALE, 22, "a design student", false, false, SleepSchedule.NIGHT_OWL),
            new Host("Sam", "Patel", Gender.NON_BINARY, 27, "a lab researcher", true, false, SleepSchedule.FLEXIBLE),
            new Host("Olivia", "Brooks", Gender.FEMALE, 35, "a UX designer", true, false, SleepSchedule.EARLY_BIRD),
            new Host("Noah", "Fischer", Gender.MALE, 26, "an MBA student", false, false, SleepSchedule.FLEXIBLE),
            new Host("Fatima", "Haddad", Gender.FEMALE, 30, "a pharmacist", false, false, SleepSchedule.EARLY_BIRD),
            new Host("Kevin", "Tran", Gender.MALE, 24, "a barista and musician", true, true, SleepSchedule.NIGHT_OWL),
            new Host("Isabel", "Costa", Gender.FEMALE, 28, "a law student", false, false, SleepSchedule.NIGHT_OWL),
            new Host("Tyler", "Wright", Gender.MALE, 32, "a startup founder", false, false, SleepSchedule.FLEXIBLE),
            new Host("Zara", "Ahmed", Gender.FEMALE, 25, "a biotech associate", true, false, SleepSchedule.EARLY_BIRD),
            new Host("Leo", "Martins", Gender.MALE, 29, "a graphic designer", true, false, SleepSchedule.NIGHT_OWL),
            new Host("Chloe", "Dubois", Gender.FEMALE, 23, "a nursing student", false, false, SleepSchedule.FLEXIBLE));

    private static final String[] HOUSEMATE_JOBS = {
            "a grad student", "a nurse", "a software engineer", "a teacher", "a lab researcher", "a designer",
            "an MBA student", "a medical resident", "a data analyst", "a musician", "a law student",
            "a project manager", "a PhD student", "a chef", "an accountant"};

    private static final String[] ROOM_FEATURES = {
            "a big closet", "a desk by the window", "hardwood floors",
            "lots of natural light", "a ceiling fan", "built-in shelves", "a window AC unit", "a dresser",
            "a reading nook", "blackout curtains", "two big windows", "its own small balcony"};

    private static final String[] BEDS = {"a queen bed", "a full-size bed", "a twin XL bed", "a queen bed frame and mattress"};

    private static final String[] HOUSE_HABITS = {
            "We keep the kitchen and bathroom clean on a simple weekly rotation.",
            "Quiet hours start at 11pm on weeknights; weekends are more relaxed.",
            "We cook together now and then, but everyone keeps their own routine.",
            "Friends are welcome to visit, just give everyone a heads-up for overnight guests.",
            "Most of us work during the day, so the apartment is calm and good for studying.",
            "We share basics like olive oil, spices and cleaning supplies and split the cost.",
            "It's a friendly, low-drama household. We have a group chat for bills and chores."};

    private static final String[] SUBLET_REASONS = {
            "One of us is away on co-op, so her room is free for a few months.",
            "I'm traveling for work and subletting my room while I'm gone.",
            "Our roommate is studying abroad this term, so the room is open.",
            "Ideal for an intern, a visiting researcher or anyone between leases.",
            "The room comes fully furnished: bed, linens, desk and kitchen basics."};

    private static final String[] NUMBERS = {"Zero", "One", "Two", "Three", "Four", "Five"};

    // ---------------------------------------------------------------- entry points

    /** First launch: create everything. */
    static void populate(DataStore store) {
        createEverything(store);
        store.save();
    }

    /**
     * An older data file was loaded. Replace the demo listings with the current set, but keep
     * real users' accounts, listings and messages. Old area names are mapped to the new ones.
     */
    static void upgrade(DataStore store) {
        store.removeListingsWithoutSaving(listing -> isDemoAccount(listing.getOwnerEmail()));
        for (Listing listing : store.getAllListings()) {
            String renamed = Neighborhoods.RENAMED.get(listing.getLocation());
            if (renamed != null) {
                listing.setLocation(renamed);
            }
        }
        createEverything(store);
    }

    private static boolean isDemoAccount(String email) {
        return email.toLowerCase().endsWith(DEMO_DOMAIN);
    }

    private static void createEverything(DataStore store) {
        PhotoLibrary photos = new PhotoLibrary(store.getSamplePhotoDir());
        Random random = new Random(6200);

        User alex = demoUser(store, "alex@demo.com", "Alex Chen", user -> {
            user.setAge(23);
            user.setGender(Gender.MALE);
            user.setPhone("(617) 555-0101");
            user.setSleepSchedule(SleepSchedule.NIGHT_OWL);
            user.setBio("MS Information Systems student at Northeastern. Quiet, tidy, and usually at the library.");
        });
        User priya = demoUser(store, "priya@demo.com", "Priya Sharma", user -> {
            user.setAge(26);
            user.setGender(Gender.FEMALE);
            user.setPhone("(617) 555-0142");
            user.setHasPets(true);
            user.setSleepSchedule(SleepSchedule.EARLY_BIRD);
            user.setBio("Software engineer in Back Bay. I have a very chill cat named Mango and love cooking on weekends.");
        });
        User jordan = demoUser(store, "jordan@demo.com", "Jordan Lee", user -> {
            user.setAge(29);
            user.setGender(Gender.NON_BINARY);
            user.setHasPets(true);
            user.setSleepSchedule(SleepSchedule.FLEXIBLE);
            user.setBio("Grad researcher and part-time barista. Our houses are social but respectful of quiet hours.");
        });

        List<User> hosts = new ArrayList<>(List.of(alex, priya, jordan));
        Map<User, Host> hostProfiles = new HashMap<>();
        for (int i = 0; i < HOSTS.size(); i++) {
            Host host = HOSTS.get(i);
            String email = (host.first() + "." + host.last()).toLowerCase().replace("'", "") + DEMO_DOMAIN;
            User user = demoUser(store, email, host.first() + " " + host.last(), u -> {
                u.setAge(host.age());
                u.setGender(host.gender());
                u.setHasPets(host.pets());
                u.setSmoker(host.smoker());
                u.setSleepSchedule(host.sleep());
                u.setPhone(String.format("(617) 555-%04d", 200 + random.nextInt(9000)));
                u.setBio("I'm " + host.job() + " in Boston. "
                        + (host.pets() ? "I have a pet, so expect some fur. " : "")
                        + "Happy to answer any questions about the place.");
            });
            hosts.add(user);
            hostProfiles.put(user, host);
        }

        Listing huntington = addHandWrittenListings(store, photos, alex, priya, jordan);
        generateListings(store, photos, random, hosts, hostProfiles);

        store.sendMessage(new Message(jordan.getEmail(), jordan.getName(), alex.getEmail(), huntington.getId(),
                huntington.getTitle(),
                "Hi Alex! A friend of mine is visiting Boston over the holidays and is interested in your room. "
                        + "Is it still available for the full period?",
                LocalDateTime.now().minusHours(5)));
    }

    /** Returns the existing demo account (keeping any edits made to it) or creates it. */
    private static User demoUser(DataStore store, String email, String name, java.util.function.Consumer<User> setup) {
        return store.findUser(email).orElseGet(() -> {
            User user = new User(DataStore.normalizeEmail(email), name, PasswordHasher.hash(DEMO_PASSWORD));
            setup.accept(user);
            store.addUserWithoutSaving(user);
            return user;
        });
    }

    // ---------------------------------------------------------------- hand-written listings

    /** The original showcase listings. Returns the Huntington Ave sublet, which the sample message is about. */
    private static Listing addHandWrittenListings(DataStore store, PhotoLibrary photos, User alex, User priya, User jordan) {
        LocalDate today = LocalDate.now();
        Random random = new Random(42);
        int age = 0;

        PermanentListing backBay = permanent(priya, age++, "Sunny private room in a Back Bay brownstone", "Back Bay",
                RoomType.PRIVATE, 2, 1, 1350, 25, 40, 15, today.plusDays(14), 12,
                prefs(true, false, GenderPreference.ANY, 21, 35));
        backBay.setDescription("Two young professionals (and Mango the cat) in a bright third-floor brownstone apartment. "
                + "We cook together a couple of times a week, keep common areas clean, and are quiet after 11pm on weeknights. "
                + "The room has a bay window, a queen bed frame and a large closet. Five minutes to the Green Line.");

        TemporaryListing fenway = temporary(priya, age++, "Summer sublet near Northeastern", "Fenway",
                RoomType.PRIVATE, 3, 1, 48, 20, 30, 10, today.plusDays(7), today.plusDays(97),
                prefs(false, false, GenderPreference.FEMALE, 18, 30));
        fenway.setDescription("Three grad students sharing a four-bedroom apartment on Peterborough St. One of us is away on co-op, "
                + "so her furnished room is free for the summer. Walking distance to campus, Fenway Park and the Muddy River.");

        PermanentListing allston = permanent(jordan, age++, "Shared room in a friendly Allston house", "Allston",
                RoomType.SHARED, 4, 2, 750, 15, 35, 0, today.plusDays(30), 6,
                prefs(true, true, GenderPreference.ANY, 0, 0));
        allston.setDescription("Big, social house with a backyard and a very good dog named Biscuit. Housemates are students and "
                + "musicians; we host the occasional dinner party. The shared room is spacious with two beds and two desks. "
                + "Free in-unit laundry.");

        PermanentListing jp = permanent(jordan, age++, "Quiet Jamaica Plain apartment by the Pond", "Jamaica Plain",
                RoomType.PRIVATE, 1, 1, 1100, 20, 45, 20, today.plusDays(3), 12,
                prefs(true, false, GenderPreference.ANY, 25, 40));
        jp.setDescription("Calm two-bedroom near Jamaica Pond. I work from home most days, so I'm looking for someone who "
                + "appreciates a peaceful space. Plants everywhere, a well-stocked kitchen, and a small balcony.");

        TemporaryListing roxbury = temporary(jordan, age++, "Short-term beds for co-op students", "Roxbury",
                RoomType.SHARED, 2, 2, 30, 15, 25, 10, today.plusDays(1), today.plusDays(180),
                prefs(false, false, GenderPreference.ANY, 18, 0));
        roxbury.setDescription("Affordable shared room for students on a co-op or internship term. Furnished, with fast Wi-Fi and "
                + "a desk for each person. Close to the Orange Line and Ruggles.");

        PermanentListing brookline = permanent(priya, age++, "Brookline 3BR with one room left", "Brookline",
                RoomType.PRIVATE, 2, 1, 1250, 30, 50, 0, today.plusDays(45), 12,
                prefs(false, false, GenderPreference.ANY, 22, 35));
        brookline.setDescription("Two nurses who work alternating shifts, so the place is often quiet. Near Coolidge Corner with "
                + "a laundromat across the street. We're looking for someone clean, reliable and non-smoking.");

        TemporaryListing huntington = temporary(alex, age++, "Winter break sublet on Huntington Ave", "Fenway",
                RoomType.PRIVATE, 1, 1, 55, 20, 30, 15, today.plusDays(20), today.plusDays(50),
                prefs(false, false, GenderPreference.MALE, 18, 30));
        huntington.setDescription("I'm heading home over winter break and my room is free. You'd share the apartment with my "
                + "roommate, a quiet PhD student. Two minutes from the E line and campus.");

        PermanentListing dorchester = permanent(jordan, age++, "Budget-friendly room in a Dorchester triple-decker", "Dorchester",
                RoomType.PRIVATE, 2, 1, 850, 20, 40, 10, today.plusDays(10), 12,
                prefs(true, false, GenderPreference.ANY, 0, 0));
        dorchester.setDescription("Top floor of a classic triple-decker with a porch. Housemates are a teacher and a nursing "
                + "student. Street parking is easy and the Red Line is a 10 minute walk.");

        TemporaryListing newbury = temporary(priya, age, "Furnished room on Newbury Street (1–3 months)", "Back Bay",
                RoomType.PRIVATE, 1, 1, 70, 25, 35, 20, today.plusDays(5), today.plusDays(95),
                prefs(false, false, GenderPreference.FEMALE, 21, 0));
        newbury.setDescription("Stylish furnished room above the shops on Newbury. Ideal for an intern or visiting researcher. "
                + "The other resident is a designer who travels often.");

        for (Listing listing : List.of(backBay, fenway, allston, jp, roxbury, brookline, huntington, dorchester, newbury)) {
            listing.setPhotoPaths(photos.forListing(areaNamed(listing.getLocation()).slug(), random));
            store.addListingWithoutSaving(listing);
        }
        return huntington;
    }

    private static Area areaNamed(String name) {
        return AREAS.stream().filter(a -> a.name().equals(name)).findFirst().orElseThrow();
    }

    // ---------------------------------------------------------------- generated listings

    private static void generateListings(DataStore store, PhotoLibrary photos, Random random, List<User> hosts,
            Map<User, Host> hostProfiles) {
        LocalDate today = LocalDate.now();
        Map<Area, Integer> perArea = new LinkedHashMap<>();
        for (int i = 0; i < GENERATED_LISTINGS; i++) {
            Area area = AREAS.get(i % AREAS.size());
            int nth = perArea.merge(area, 1, Integer::sum) - 1; // cycles title templates within an area
            User owner = hosts.get(random.nextInt(hosts.size()));
            Host profile = hostProfiles.get(owner);
            boolean temporary = random.nextInt(100) < 30;
            RoomType roomType = random.nextInt(100) < 18 ? RoomType.SHARED : RoomType.PRIVATE;
            String housing = area.housing()[random.nextInt(area.housing().length)];

            int residents = roomType == RoomType.SHARED ? 2 + random.nextInt(3) : 1 + random.nextInt(4);
            int spots = roomType == RoomType.SHARED ? 1 + random.nextInt(2) : (random.nextInt(100) < 80 ? 1 : 2);
            double monthly = roundTo(25, area.minRent() + random.nextInt(area.maxRent() - area.minRent() + 1));
            if (roomType == RoomType.SHARED) {
                monthly = roundTo(25, monthly * 0.6);
            }
            double wifi = pick(random, 15, 20, 25, 30, 35);
            double electricity = 25 + 5 * random.nextInt(10);
            double laundry = random.nextInt(100) < 35 ? 0 : pick(random, 10, 15, 20, 25);
            LocalDate from = today.plusDays(random.nextInt(76));
            HouseholdPreferences preferences = randomPreferences(random, owner);
            int daysAgo = random.nextInt(60);

            Listing listing;
            if (temporary) {
                double daily = Math.round(monthly / 30 * 1.3);
                LocalDate until = from.plusDays((long) pick(random, 30, 45, 60, 90, 120));
                listing = temporary(owner, daysAgo, temporaryTitle(area, housing, nth), area.name(), roomType,
                        residents, spots, daily, wifi, electricity, laundry, from, until, preferences);
            } else {
                int lease = (int) pick(random, 12, 12, 12, 9, 6);
                String title = roomType == RoomType.SHARED ? sharedTitle(area, housing, nth) : privateTitle(area, housing, nth);
                listing = permanent(owner, daysAgo, title, area.name(), roomType, residents, spots, monthly,
                        wifi, electricity, laundry, from, lease, preferences);
            }
            listing.setDescription(describe(random, area, housing, owner, profile, residents, roomType, temporary,
                    laundry == 0));
            listing.setPhotoPaths(photos.forListing(area.slug(), random));
            store.addListingWithoutSaving(listing);
        }
    }

    private static String privateTitle(Area area, String housing, int nth) {
        String[] titles = {
                "Bright private room in " + area.name(),
                "Private room in a " + housing + " near " + area.landmark(),
                "Sunny room with lots of storage in " + area.name(),
                "Quiet room in a " + area.name() + " " + housing,
                "Spacious room with big windows, " + area.name(),
                "Cozy private room near " + area.landmark(),
                "Furnished room in a friendly " + area.name() + " apartment"};
        return titles[nth % titles.length];
    }

    private static String sharedTitle(Area area, String housing, int nth) {
        String[] titles = {
                "Shared room in a friendly " + area.name() + " " + housing,
                "Affordable shared room near " + area.landmark(),
                "Budget-friendly shared room in " + area.name()};
        return titles[nth % titles.length];
    }

    private static String temporaryTitle(Area area, String housing, int nth) {
        String[] titles = {
                "Summer sublet in " + area.name(),
                "Furnished short stay near " + area.landmark(),
                "Semester sublet in a " + area.name() + " " + housing,
                "Co-op term room in " + area.name(),
                "Month-to-month room near " + area.landmark()};
        return titles[nth % titles.length];
    }

    private static String describe(Random random, Area area, String housing, User owner, Host profile, int residents,
            RoomType roomType, boolean temporary, boolean freeLaundry) {
        StringBuilder text = new StringBuilder();
        if (temporary) {
            text.append(SUBLET_REASONS[random.nextInt(SUBLET_REASONS.length)]).append(' ');
        }

        String ownerJob = profile != null ? profile.job() : "a grad student";
        if (residents == 1) {
            text.append("I'm ").append(ownerJob).append(" and live here on my own in a ").append(housing)
                    .append(", looking for good company. ");
        } else {
            List<String> people = new ArrayList<>(List.of(ownerJob));
            while (people.size() < residents) {
                String job = HOUSEMATE_JOBS[random.nextInt(HOUSEMATE_JOBS.length)];
                if (!people.contains(job)) {
                    people.add(job);
                }
            }
            text.append(NUMBERS[residents]).append(" of us share this ").append(housing).append(": ")
                    .append(joinWithAnd(people)).append(". ");
        }

        List<String> features = new ArrayList<>(List.of(BEDS[random.nextInt(BEDS.length)]));
        while (features.size() < 3) {
            String feature = ROOM_FEATURES[random.nextInt(ROOM_FEATURES.length)];
            if (!features.contains(feature)) {
                features.add(feature);
            }
        }
        text.append(roomType == RoomType.SHARED ? "The shared room is roomy and has " : "The room has ")
                .append(joinWithAnd(features)).append(". ");
        if (freeLaundry) {
            text.append("Laundry is in the unit and free. ");
        }
        text.append(HOUSE_HABITS[random.nextInt(HOUSE_HABITS.length)]).append(' ');
        if (owner.hasPets()) {
            text.append(random.nextBoolean() ? "There's a friendly cat in the house. " : "We have a sweet, well-trained dog. ");
        }
        text.append("It's about a ").append(3 + random.nextInt(10)).append("-minute walk to ").append(area.transit())
                .append(", and you're close to ").append(area.nearby()).append('.');
        return text.toString();
    }

    private static HouseholdPreferences randomPreferences(Random random, User owner) {
        boolean pets = owner.hasPets() || random.nextInt(100) < 25;
        boolean smoking = owner.isSmoker() && random.nextInt(100) < 60;
        GenderPreference gender = GenderPreference.ANY;
        if (owner.getGender() == Gender.FEMALE && random.nextInt(100) < 22) {
            gender = GenderPreference.FEMALE;
        } else if (owner.getGender() == Gender.MALE && random.nextInt(100) < 12) {
            gender = GenderPreference.MALE;
        }
        int minAge = 0;
        int maxAge = 0;
        if (random.nextInt(100) < 35) {
            minAge = (int) pick(random, 18, 21, 22, 25);
            maxAge = (int) pick(random, 30, 32, 35, 40);
        }
        return prefs(pets, smoking, gender, minAge, maxAge);
    }

    private static String joinWithAnd(List<String> items) {
        if (items.size() == 1) {
            return items.get(0);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.get(items.size() - 1);
    }

    private static double pick(Random random, double... values) {
        return values[random.nextInt(values.length)];
    }

    private static double roundTo(int step, double value) {
        return Math.round(value / step) * step;
    }

    // ---------------------------------------------------------------- builders

    private static PermanentListing permanent(User owner, int daysAgo, String title, String location, RoomType roomType,
            int residents, int spots, double rent, double wifi, double electricity, double laundry,
            LocalDate from, int leaseMonths, HouseholdPreferences preferences) {
        PermanentListing listing = new PermanentListing(UUID.randomUUID().toString(), owner.getEmail(),
                LocalDateTime.now().minusDays(daysAgo).minusMinutes(daysAgo * 37L));
        listing.setMonthlyRent(rent);
        listing.setMinLeaseMonths(leaseMonths);
        fill(listing, title, location, roomType, residents, spots, wifi, electricity, laundry, from, preferences);
        return listing;
    }

    private static TemporaryListing temporary(User owner, int daysAgo, String title, String location, RoomType roomType,
            int residents, int spots, double dailyRate, double wifi, double electricity, double laundry,
            LocalDate from, LocalDate until, HouseholdPreferences preferences) {
        TemporaryListing listing = new TemporaryListing(UUID.randomUUID().toString(), owner.getEmail(),
                LocalDateTime.now().minusDays(daysAgo).minusMinutes(daysAgo * 37L));
        listing.setDailyRate(dailyRate);
        listing.setAvailableUntil(until);
        fill(listing, title, location, roomType, residents, spots, wifi, electricity, laundry, from, preferences);
        return listing;
    }

    private static void fill(Listing listing, String title, String location, RoomType roomType, int residents, int spots,
            double wifi, double electricity, double laundry, LocalDate from, HouseholdPreferences preferences) {
        listing.setTitle(title);
        listing.setLocation(location);
        listing.setRoomType(roomType);
        listing.setCurrentResidents(residents);
        listing.setOpenSpots(spots);
        listing.setWifiCost(wifi);
        listing.setElectricityCost(electricity);
        listing.setLaundryCost(laundry);
        listing.setAvailableFrom(from);
        listing.setPreferences(preferences);
    }

    private static HouseholdPreferences prefs(boolean pets, boolean smoking, GenderPreference gender, int minAge, int maxAge) {
        HouseholdPreferences preferences = new HouseholdPreferences();
        preferences.setPetFriendly(pets);
        preferences.setSmokingAllowed(smoking);
        preferences.setGenderPreference(gender);
        preferences.setMinAge(minAge);
        preferences.setMaxAge(maxAge);
        return preferences;
    }

    // ---------------------------------------------------------------- photos

    /**
     * The bundled sample photos, listed in {@code /images/sample/manifest.txt} as
     * {@code <kind> <group> <file>} lines, where kind is "area" (group = neighborhood slug) or
     * "room" (group = bedroom, living or kitchen). Files are copied into the data folder the
     * first time they're needed, because listings store photos as file paths.
     */
    private static final class PhotoLibrary {
        private final Path folder;
        private final Map<String, List<String>> areaPhotos = new HashMap<>();
        private final Map<String, List<String>> roomPhotos = new HashMap<>();
        private final Map<String, Integer> areaTurn = new HashMap<>();

        PhotoLibrary(Path folder) {
            this.folder = folder;
            InputStream manifest = SampleData.class.getResourceAsStream("/images/sample/manifest.txt");
            if (manifest == null) {
                System.err.println("Sample photos are missing; demo listings will use placeholders.");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(manifest, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length != 3 || parts[0].startsWith("#")) {
                        continue;
                    }
                    Map<String, List<String>> target = parts[0].equals("area") ? areaPhotos : roomPhotos;
                    target.computeIfAbsent(parts[1], key -> new ArrayList<>()).add(parts[2]);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read the sample photo list", e);
            }
        }

        /** A neighborhood photo first (taking turns between that area's photos), then three room photos. */
        List<String> forListing(String areaSlug, Random random) {
            List<String> chosen = new ArrayList<>();
            List<String> area = areaPhotos.getOrDefault(areaSlug, List.of());
            if (!area.isEmpty()) {
                int turn = areaTurn.merge(areaSlug, 1, Integer::sum) - 1;
                chosen.add(area.get(turn % area.size()));
            }
            for (String kind : new String[] {"bedroom", "living", "kitchen"}) {
                List<String> options = roomPhotos.getOrDefault(kind, List.of());
                if (!options.isEmpty()) {
                    chosen.add(options.get(random.nextInt(options.size())));
                }
            }
            List<String> paths = new ArrayList<>();
            for (String file : chosen) {
                String path = install(file);
                if (path != null) {
                    paths.add(path);
                }
            }
            return paths;
        }

        private String install(String file) {
            Path target = folder.resolve(file);
            if (!Files.exists(target)) {
                try (InputStream in = SampleData.class.getResourceAsStream("/images/sample/" + file)) {
                    if (in == null) {
                        return null;
                    }
                    Files.createDirectories(target.getParent());
                    Files.copy(in, target);
                } catch (IOException e) {
                    System.err.println("Could not copy sample photo " + file + ": " + e.getMessage());
                    return null;
                }
            }
            return target.toAbsolutePath().toString();
        }
    }
}
