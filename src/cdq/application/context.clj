(ns cdq.application.context
  (:require [cdq.malli :as m]
            [qrecord.core :as q]))

(q/defrecord Context [ctx/app
                      ctx/files
                      ctx/config
                      ctx/input
                      ctx/db
                      ctx/audio
                      ctx/stage
                      ctx/graphics
                      ctx/world])

(def schema [:map {:closed true}
             [:ctx/app :some]
             [:ctx/files :some]
             [:ctx/config :some]
             [:ctx/input :some]
             [:ctx/db :some]
             [:ctx/audio :some]
             [:ctx/stage :some]
             [:ctx/graphics :some]
             [:ctx/world :some]])

(defn create []
  (map->Context {}))

(defn validate [ctx]
  (m/validate-humanize schema ctx)
  ctx)
