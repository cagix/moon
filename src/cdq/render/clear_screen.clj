(ns cdq.render.clear-screen
  (:require [clojure.gdx.utils.screen :as screen-utils]))

(defn do! [ctx]
  (screen-utils/clear! :black)
  ctx)
