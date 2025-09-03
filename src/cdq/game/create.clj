(ns cdq.game.create
  (:require [cdq.ctx.audio :as audio]
            [cdq.ctx.input :as input]
            [cdq.db-impl :as db]
            [cdq.gdx.ui :as ui]
            [cdq.malli :as m]
            [qrecord.core :as q]))

(q/defrecord Context [ctx/config
                      ctx/input
                      ctx/db
                      ctx/audio
                      ctx/stage
                      ctx/mouseover-eid
                      ctx/player-eid
                      ctx/graphics
                      ctx/world])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/config :some]
             [:ctx/input :some]
             [:ctx/db :some]
             [:ctx/audio :some]
             [:ctx/stage :some]
             [:ctx/mouseover-eid :any]
             [:ctx/player-eid :some]
             [:ctx/graphics :some]
             [:ctx/world :some]]))

(defn validate [ctx]
  (m/validate-humanize schema ctx)
  ctx)

(defn do! [gdx config]
  (ui/load! (::stage config))
  (let [input (:input gdx)
        graphics ((requiring-resolve (:graphics-impl config)) gdx (::graphics config))
        stage (ui/stage (:ui-viewport graphics)
                        (:batch       graphics))]
    (input/set-processor! input stage)
    (-> (map->Context {:audio (audio/create gdx (::audio config))
                       :config config
                       :db (db/create (::db config))
                       :graphics graphics
                       :input input
                       :stage stage})
        ((requiring-resolve (:reset-game-state! config)) (::starting-level config))
        (assoc :ctx/mouseover-eid nil)
        validate)))
