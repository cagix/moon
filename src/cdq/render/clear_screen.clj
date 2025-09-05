(ns cdq.render.clear-screen
  (:require [clojure.gdx.utils.screen :as screen]))

(defn do!
  [_ctx]
  (screen/clear!))
