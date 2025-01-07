(ns gdl.context.batch
  (:require [clojure.gdx :as gdx]))

(defn dispose [[_ batch]]
  (gdx/dispose batch))
