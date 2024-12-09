(ns forge.app.default-font
  (:require [anvil.disposable :as disposable]
            [anvil.graphics :refer [default-font]]
            [clojure.gdx.graphics.g2d.freetype :as freetype]
            [clojure.utils :refer [bind-root]]))

(defn create [[_ font]]
  (bind-root default-font (freetype/generate-font font)))

(defn dispose [_]
  (disposable/dispose default-font))
