package nurgling.cheese;

import java.util.*;

public class CheeseBranch {
    public enum Place { start, cellar, inside, outside, mine }

    public static class Cheese {
        public Place place;
        public String name;
        public Cheese(Place place, String name) {
            this.place = place;
            this.name = name;
        }
    }

    public List<Cheese> steps;
    public CheeseBranch(List<Cheese> steps) {
        this.steps = steps;
    }

    public static final List<CheeseBranch> branches = new ArrayList<>();

    static {
        if(branches.isEmpty()) {
            LinkedList<CheeseBranch.Cheese> creamy_camembert = new LinkedList<>();
            creamy_camembert.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Cow's Curd"));
            creamy_camembert.add(new CheeseBranch.Cheese(CheeseBranch.Place.outside, "Creamy Camembert"));
            branches.add(new CheeseBranch(creamy_camembert));

            LinkedList<CheeseBranch.Cheese> musky_milben = new LinkedList<>();
            musky_milben.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Cow's Curd"));
            musky_milben.add(new CheeseBranch.Cheese(CheeseBranch.Place.inside, "Tasty Emmentaler"));
            musky_milben.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Musky Milben"));
            branches.add(new CheeseBranch(musky_milben));

            LinkedList<CheeseBranch.Cheese> midnight_blue = new LinkedList<>();
            midnight_blue.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Cow's Curd"));
            midnight_blue.add(new CheeseBranch.Cheese(CheeseBranch.Place.cellar, "Cellar Cheddar"));
            midnight_blue.add(new CheeseBranch.Cheese(CheeseBranch.Place.outside, "Brodgar Blue Cheese"));
            midnight_blue.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Jorbonzola"));
            midnight_blue.add(new CheeseBranch.Cheese(CheeseBranch.Place.cellar, "Midnight Blue Cheese"));
            branches.add(new CheeseBranch(midnight_blue));

            LinkedList<CheeseBranch.Cheese> cave_chedar = new LinkedList<>();
            cave_chedar.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Cow's Curd"));
            cave_chedar.add(new CheeseBranch.Cheese(CheeseBranch.Place.cellar, "Cellar Cheddar"));
            cave_chedar.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Cave Cheddar"));
            branches.add(new CheeseBranch(cave_chedar));

            LinkedList<CheeseBranch.Cheese> sunlight = new LinkedList<>();
            sunlight.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Cow's Curd"));
            sunlight.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Mothzarella"));
            sunlight.add(new CheeseBranch.Cheese(CheeseBranch.Place.inside, "Harmesan Cheese"));
            sunlight.add(new CheeseBranch.Cheese(CheeseBranch.Place.outside, "Sunlit Stilton"));
            branches.add(new CheeseBranch(sunlight));

            LinkedList<CheeseBranch.Cheese> halloumi = new LinkedList<>();
            halloumi.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Sheep's Curd"));
            halloumi.add(new CheeseBranch.Cheese(CheeseBranch.Place.inside, "Halloumi"));
            branches.add(new CheeseBranch(halloumi));

            LinkedList<CheeseBranch.Cheese> caciotta = new LinkedList<>();
            caciotta.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Sheep's Curd"));
            caciotta.add(new CheeseBranch.Cheese(CheeseBranch.Place.cellar, "Feta"));
            caciotta.add(new CheeseBranch.Cheese(CheeseBranch.Place.inside, "Caciotta"));
            branches.add(new CheeseBranch(caciotta));

            LinkedList<CheeseBranch.Cheese> cabrales = new LinkedList<>();
            cabrales.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Sheep's Curd"));
            cabrales.add(new CheeseBranch.Cheese(CheeseBranch.Place.cellar, "Feta"));
            cabrales.add(new CheeseBranch.Cheese(CheeseBranch.Place.outside, "Cabrales"));
            branches.add(new CheeseBranch(cabrales));

            LinkedList<CheeseBranch.Cheese> manchego = new LinkedList<>();
            manchego.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Sheep's Curd"));
            manchego.add(new CheeseBranch.Cheese(CheeseBranch.Place.outside, "Pecorino"));
            manchego.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Manchego"));
            branches.add(new CheeseBranch(manchego));

            LinkedList<CheeseBranch.Cheese> roncal = new LinkedList<>();
            roncal.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Sheep's Curd"));
            roncal.add(new CheeseBranch.Cheese(CheeseBranch.Place.outside, "Pecorino"));
            roncal.add(new CheeseBranch.Cheese(CheeseBranch.Place.cellar, "Gbejna"));
            roncal.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Roncal"));
            branches.add(new CheeseBranch(roncal));

            LinkedList<CheeseBranch.Cheese> oscypki = new LinkedList<>();
            oscypki.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Sheep's Curd"));
            oscypki.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Abbaye"));
            oscypki.add(new CheeseBranch.Cheese(CheeseBranch.Place.cellar, "Zamorano"));
            oscypki.add(new CheeseBranch.Cheese(CheeseBranch.Place.inside, "Brique"));
            oscypki.add(new CheeseBranch.Cheese(CheeseBranch.Place.outside, "Oscypki"));
            branches.add(new CheeseBranch(oscypki));

            LinkedList<CheeseBranch.Cheese> robiola = new LinkedList<>();
            robiola.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Goat's Curd"));
            robiola.add(new CheeseBranch.Cheese(CheeseBranch.Place.inside, "Banon"));
            robiola.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Robiola"));
            branches.add(new CheeseBranch(robiola));

            LinkedList<CheeseBranch.Cheese> picodon = new LinkedList<>();
            picodon.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Goat's Curd"));
            picodon.add(new CheeseBranch.Cheese(CheeseBranch.Place.outside, "Bucheron"));
            picodon.add(new CheeseBranch.Cheese(CheeseBranch.Place.cellar, "Picodon"));
            branches.add(new CheeseBranch(picodon));

            LinkedList<CheeseBranch.Cheese> garrotxa = new LinkedList<>();
            garrotxa.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Goat's Curd"));
            garrotxa.add(new CheeseBranch.Cheese(CheeseBranch.Place.outside, "Bucheron"));
            garrotxa.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Graviera"));
            garrotxa.add(new CheeseBranch.Cheese(CheeseBranch.Place.inside, "Gevrik"));
            garrotxa.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Garrotxa"));
            branches.add(new CheeseBranch(garrotxa));

            LinkedList<CheeseBranch.Cheese> formaela = new LinkedList<>();
            formaela.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Goat's Curd"));
            formaela.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Chabichou"));
            formaela.add(new CheeseBranch.Cheese(CheeseBranch.Place.inside, "Chabis"));
            formaela.add(new CheeseBranch.Cheese(CheeseBranch.Place.cellar, "Formaela"));
            branches.add(new CheeseBranch(formaela));

            LinkedList<CheeseBranch.Cheese> majorero = new LinkedList<>();
            majorero.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Goat's Curd"));
            majorero.add(new CheeseBranch.Cheese(CheeseBranch.Place.mine, "Chabichou"));
            majorero.add(new CheeseBranch.Cheese(CheeseBranch.Place.outside, "Majorero"));
            branches.add(new CheeseBranch(majorero));

            LinkedList<CheeseBranch.Cheese> kasseri = new LinkedList<>();
            kasseri.add(new CheeseBranch.Cheese(CheeseBranch.Place.start, "Goat's Curd"));
            kasseri.add(new CheeseBranch.Cheese(CheeseBranch.Place.cellar, "Kasseri"));
            branches.add(new CheeseBranch(kasseri));
        }
    }

    public static List<Cheese> getChainToProduct(String productName) {
        for (CheeseBranch b : branches) {
            List<Cheese> out = new ArrayList<>();
            for (Cheese step : b.steps) {
                out.add(step);
                if (step.name.equals(productName))
                    return out;
            }
        }
        return null;
    }

    public static List<String> allProducts() {
        Set<String> out = new HashSet<>();
        for (CheeseBranch b : branches)
            for (Cheese c : b.steps)
                if (!c.name.trim().equalsIgnoreCase("Cow's Curd") &&
                        !c.name.trim().equalsIgnoreCase("Goat's Curd") &&
                        !c.name.trim().equalsIgnoreCase("Sheep's Curd"))
                    out.add(c.name.trim());
        List<String> sorted = new ArrayList<>(out);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }
}
