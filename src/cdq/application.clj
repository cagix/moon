(ns cdq.application
  (:require [cdq.malli :as m]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx.utils Disposable)))

(q/defrecord Context [ctx/assets
                      ctx/batch
                      ctx/config
                      ctx/cursors
                      ctx/db
                      ctx/default-font
                      ctx/graphics
                      ctx/input
                      ctx/stage
                      ctx/ui-viewport
                      ctx/unit-scale
                      ctx/shape-drawer
                      ctx/shape-drawer-texture
                      ctx/tiled-map-renderer
                      ctx/world-unit-scale
                      ctx/world-viewport])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/assets :some]
             [:ctx/batch :some]
             [:ctx/config :some]
             [:ctx/cursors :some]
             [:ctx/db :some]
             [:ctx/default-font :some]
             [:ctx/graphics :some]
             [:ctx/input :some]
             [:ctx/stage :some]
             [:ctx/ui-viewport :some]
             [:ctx/unit-scale :some]
             [:ctx/shape-drawer :some]
             [:ctx/shape-drawer-texture :some]
             [:ctx/tiled-map-renderer :some]
             [:ctx/world-unit-scale :some]
             [:ctx/world-viewport :some]
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

(defn- create* [config]
  (let [ctx (map->Context {:config config})
        ctx (reduce (fn [ctx f]
                      (f ctx))
                    ctx
                    (:create-fns config))]
    (m/validate-humanize schema ctx)
    ctx))

(defn- dispose* [{:keys [ctx/assets
                         ctx/batch
                         ctx/cursors
                         ctx/default-font
                         ctx/shape-drawer-texture]}]
  (Disposable/.dispose assets)
  (Disposable/.dispose batch)
  (run! Disposable/.dispose (vals cursors))
  (Disposable/.dispose default-font)
  (Disposable/.dispose shape-drawer-texture)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )

(defn- render* [{:keys [ctx/config] :as ctx}]
  (m/validate-humanize schema ctx)
  (let [ctx (reduce (fn [ctx render!]
                      (render! ctx))
                    ctx
                    (:render-fns config))]
    (m/validate-humanize schema ctx)
    ctx))

(defprotocol Resizable
  (resize! [_ width height]))

(defn- resize* [{:keys [ctx/ui-viewport
                        ctx/world-viewport]}
                width
                height]
  (resize! ui-viewport    width height)
  (resize! world-viewport width height))

(def state (atom nil))

(defn create! [config]
  (reset! state (create* config)))

(defn dispose! []
  (dispose* @state))

(defn render! []
  (swap! state render*))

(defn resize! [width height]
  (resize* @state width height))
