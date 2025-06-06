(ns clojure.gdx.graphics.g2d.batch
  (:import (com.badlogic.gdx.graphics.g2d Batch)))

(defn set-color! [^Batch batch color]
  (.setColor batch ^Color color))

(defn draw! [^Batch batch
             texture-region
             {:keys [x
                     y
                     origin-x
                     origin-y
                     width
                     height
                     scale-x
                     scale-y
                     rotation]}]
  (.draw batch
         (:texture-region/java-object texture-region)
         x
         y
         origin-x
         origin-y
         width
         height
         scale-x
         scale-y
         rotation))

(defn begin! [^Batch batch]
  (.begin batch))

(defn end! [^Batch batch]
  (.end batch))

(defn set-projection-matrix! [^Batch batch matrix]
  (.setProjectionMatrix batch matrix))
