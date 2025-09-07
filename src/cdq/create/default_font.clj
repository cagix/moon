(ns cdq.create.default-font
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics.g2d.freetype :as freetype]))

(def file "exocet/films.EXL_____.ttf")
(def params {:size 16
             :quality-scaling 2
             :enable-markup? true
             ; false, otherwise scaling to world-units not visible
             :use-integer-positions? false})

(defn do!
  [ctx]
  (assoc ctx :ctx/default-font (freetype/generate-font (files/internal (gdx/files) file)
                                                       params)))
