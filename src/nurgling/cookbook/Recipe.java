package nurgling.cookbook;

import java.util.*;

public class Recipe {
    private final String hash;
    private final String name;
    private final String resourceName;
    private final double hunger;
    private final int energy;
    private final Map<String, Double> ingredients; // Ингредиент -> процент
    private final Map<String, Fep> feps;         // Название FEPS -> значение

    public Recipe(String hash, String name, String resourceName,
                  double hunger, int energy,
                  Map<String, Double> ingredients,
                  Map<String, Fep> feps) {
        this.hash = hash;
        this.name = name;
        this.resourceName = resourceName;
        this.hunger = hunger;
        this.energy = energy;
        this.ingredients = ingredients;
        this.feps = feps;
    }

    // Геттеры
    public String getHash() {
        return hash;
    }

    public String getName() {
        return name;
    }

    public String getResourceName() {
        return resourceName;
    }

    public double getHunger() {
        return hunger;
    }

    public int getEnergy() {
        return energy;
    }

    public Map<String, Double> getIngredients() {
        return ingredients;
    }

    public static class Fep
    {
        public Fep(double val, double weigth) {
            this.val = val;
            this.weigth = weigth;
        }

        public double val;
        public double weigth;
    }

    public Map<String, Fep> getFeps() {
        return feps;
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "hash='" + hash + '\'' +
                ", name='" + name + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", hunger=" + hunger +
                ", energy=" + energy +
                ", ingredients=" + ingredients +
                ", feps=" + feps.toString() +
                '}';
    }
}
