(ns cdq.create.default-font
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics.g2d.freetype :as freetype]))

(defn do!
  [ctx {:keys [file params]}]
  (assoc ctx :ctx/default-font
         (freetype/generate-font (files/internal (gdx/files) file)
                                 params)))
