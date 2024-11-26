(ns forge.graphics.color
  (:refer-clojure :exclude [munge])
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.color :as color])
  (:import (com.badlogic.gdx.graphics Colors)))

(defn munge [color]
  (cond (= com.badlogic.gdx.graphics.Color (class color)) color
        (keyword? color) (gdx/field "graphics.Color" color)
        (vector? color) (apply color/create color)
        :else (throw (ex-info "Cannot understand color" {:color color}))))

(defn put
  "A general purpose class containing named colors that can be changed at will. For example, the markup language defined by the BitmapFontCache class uses this class to retrieve colors and the user can define his own colors.

  [javadoc](https://javadoc.io/doc/com.badlogicgames.gdx/gdx/latest/com/badlogic/gdx/graphics/Colors.html)"
  [name-str color]
  (Colors/put name-str (munge color)))
