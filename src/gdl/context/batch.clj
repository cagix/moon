(ns gdl.context.batch
  (:require [clojure.gdx :as gdx]))

(defn create [_ _context]
  (gdx/sprite-batch))

(defn dispose [[_ batch]]
  (gdx/dispose batch))
