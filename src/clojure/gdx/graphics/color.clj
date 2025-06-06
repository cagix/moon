(ns clojure.gdx.graphics.color
  (:import (com.badlogic.gdx.graphics Color)))

(def ^:private mapping
  {:black       Color/BLACK
   :blue        Color/BLUE
   :brown       Color/BROWN
   :chartreuse  Color/CHARTREUSE
   :clear       Color/CLEAR
   :clear-white Color/CLEAR_WHITE
   :coral       Color/CORAL
   :cyan        Color/CYAN
   :dark-gray   Color/DARK_GRAY
   :firebrick   Color/FIREBRICK
   :forest      Color/FOREST
   :gold        Color/GOLD
   :goldenrod   Color/GOLDENROD
   :gray        Color/GRAY
   :green       Color/GREEN
   :light-gray  Color/LIGHT_GRAY
   :lime        Color/LIME
   :magenta     Color/MAGENTA
   :maroon      Color/MAROON
   :navy        Color/NAVY
   :olive       Color/OLIVE
   :orange      Color/ORANGE
   :pink        Color/PINK
   :purple      Color/PURPLE
   :red         Color/RED
   :royal       Color/ROYAL
   :salmon      Color/SALMON
   :scarlet     Color/SCARLET
   :sky         Color/SKY
   :slate       Color/SLATE
   :tan         Color/TAN
   :teal        Color/TEAL
   :violet      Color/VIOLET
   :white       Color/WHITE
   :yellow      Color/YELLOW})

(defn- k->color [k]
  (when-not (contains? mapping k)
    (throw (IllegalArgumentException. (str "Unknown Color: " k ". \nOptions are:\n" (sort (keys mapping))))))
  (k mapping))

(defn- create-color
  ([r g b]
   (create-color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))

(defn create ^Color [c]
  (cond (= Color (class c)) c
        (keyword? c) (k->color c)
        (vector? c) (apply create-color c)
        :else (throw (ex-info "Cannot understand color" c))))
