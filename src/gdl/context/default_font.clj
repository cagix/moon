(ns gdl.context.default-font
  (:require [clojure.gdx :as gdx]))

(defn dispose [[_ font]]
  (gdx/dispose font))
