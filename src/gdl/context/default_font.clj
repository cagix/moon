(ns gdl.context.default-font
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.g2d.freetype :as freetype]))

(defn create [[_ config] context]
  (freetype/generate-font (update config :file #(gdx/internal-file context %))))

(defn dispose [[_ font]]
  (gdx/dispose font))
