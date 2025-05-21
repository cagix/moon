(ns cdq.application.render.bind-active-entities
  (:require [cdq.content-grid :as content-grid]))

(defn do!
  [{:keys [ctx/content-grid
           ctx/player-eid]
    :as ctx}]
  (assoc ctx :ctx/active-entities (content-grid/active-entities content-grid
                                                                @player-eid)))
