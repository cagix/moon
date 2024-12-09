(ns forge.app.default-font
  (:require [anvil.graphics :as g]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.utils :refer [bind-root]]))

(defn create [[_ font]]
  (bind-root g/default-font (freetype/generate-font font)))

(defn dispose [_]
  (disposable/dispose g/default-font))
