(ns cdq.game
  (:require [cdq.malli :as m]
            [gdl.viewport :as viewport]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx.utils Disposable)))

(q/defrecord Context [ctx/assets
                      ctx/graphics
                      ctx/stage])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/gdx :some]
             [:ctx/assets :some]
             [:ctx/graphics :some]
             [:ctx/input :some]
             [:ctx/ui-viewport :some]
             [:ctx/stage :some]
             [:ctx/config :some]
             [:ctx/db :some]
             [:ctx/elapsed-time :some]
             [:ctx/delta-time {:optional true} number?]
             [:ctx/paused? {:optional true} :boolean]
             [:ctx/tiled-map :some]
             [:ctx/grid :some]
             [:ctx/raycaster :some]
             [:ctx/content-grid :some]
             [:ctx/explored-tile-corners :some]
             [:ctx/id-counter :some]
             [:ctx/entity-ids :some]
             [:ctx/potential-field-cache :some]
             [:ctx/factions-iterations :some]
             [:ctx/z-orders :some]
             [:ctx/render-z-order :some]
             [:ctx/mouseover-eid {:optional true} :any]
             [:ctx/player-eid :some]
             [:ctx/active-entities {:optional true} :some]]))

(def create-fns
  '[
    cdq.create.extend-types/do!
    cdq.create.gdx/do!
    cdq.create.graphics/do!
    cdq.create.input/do!
    cdq.create.ui-viewport/do!
    cdq.create.stage/do!
    cdq.create.assets/do!
    cdq.create.db/do!
    cdq.create.game-state/do!
    ])

(defn create! [config]
  (let [create-fns (map requiring-resolve create-fns)
        initial-context (map->Context {:ctx/config config})
        ctx (reduce (fn [ctx create!]
                      (create! ctx))
                    initial-context
                    create-fns)]
    (m/validate-humanize schema ctx)
    ctx))

(defn dispose! [{:keys [ctx/assets
                        ctx/graphics]}]
  (Disposable/.dispose assets)
  (Disposable/.dispose graphics)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )

(defn resize! [{:keys [ctx/ui-viewport] :as ctx}
               width
               height]
  (viewport/update! ui-viewport width height)
  (viewport/update! (:world-viewport (:ctx/graphics ctx)) width height))

(def render-fns
  '[
    cdq.render.assoc-active-entities/do!
    cdq.render.set-camera-on-player/do!
    cdq.render.clear-screen/do!
    cdq.render.draw-world-map/do!
    cdq.render.draw-on-world-viewport/do!
    cdq.render.ui/do!
    cdq.render.player-state-handle-click/do!
    cdq.render.update-mouseover-entity/do!
    cdq.render.assoc-paused/do!
    cdq.render.update-time/do!
    cdq.render.update-potential-fields/do!
    cdq.render.tick-entities/do!
    cdq.render.remove-destroyed-entities/do!
    cdq.render.camera-controls/do!
    ])

(defn render! [ctx]
  (m/validate-humanize schema ctx)
  (let [render-fns (map requiring-resolve render-fns)
        ctx (reduce (fn [ctx render!]
                      (render! ctx))
                    ctx
                    render-fns)]
    (m/validate-humanize schema ctx)
    ctx))
