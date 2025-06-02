(ns clojure.render.clear-screen
  (:require [clojure.gdx :as gdx]))

(defn do! [ctx]
  (gdx/clear-screen!)
  ctx)
