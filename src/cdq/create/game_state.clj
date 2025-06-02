(ns cdq.create.game-state
  (:require [clojure.ctx :as ctx]))

(defn do! [{:keys [ctx/config] :as ctx}]
  (ctx/reset-game-state! ctx (:world-fn config)))
