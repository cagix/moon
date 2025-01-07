(ns gdl.context.stage
  (:require [clojure.gdx :as gdx]))

(defn dispose [[_ stage]]
  (gdx/dispose stage))
