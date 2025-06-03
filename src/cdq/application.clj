(ns cdq.application
  (:require [gdl.graphics.viewport :as viewport]
            [cdq.malli :as m]
            [cdq.utils :as utils]
            [qrecord.core :as q]))

; => this whole thing is my application listener itself !?
; config == the application listener type ?!
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

; 6-8 context keys only ...

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/config :some] ; -> ?? depends on what ??
             [:ctx/db :some] ; -> part of game ?

             [:ctx/files :some]

             [:ctx/assets :some] ; - actually textures (graphics) & audio (sound )

             [:ctx/input :some] ; -> 'controls together w. config

             [:ctx/stage :some] ; -> user-interface protocol w. stage actor functions

             ;; graphics
             [:ctx/batch :some]
             [:ctx/cursors :some]
             [:ctx/default-font :some]
             [:ctx/graphics :some]
             [:ctx/ui-viewport :some]
             [:ctx/unit-scale :some]
             [:ctx/shape-drawer :some]
             [:ctx/shape-drawer-texture :some]
             [:ctx/tiled-map-renderer :some]
             [:ctx/world-unit-scale :some]
             [:ctx/world-viewport :some]
             ;;

             ; # 1 complexity - game context - make in one field ?
             ; but then there is also entity stats #1 #1 complexity !
             ; @ spawn-entity !
             ; and there the # 1 problem is body has non-namespaced keys .... fix that first
             ; and make body separate component !?


             ;; below is only world/game related
             [:ctx/elapsed-time number?]
             [:ctx/delta-time {:optional true} number?]
             [:ctx/max-delta number?]
             [:ctx/max-speed number?]
             [:ctx/minimum-size number?]
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
             [:ctx/active-entities {:optional true} :some]
             ;;
             ]))

(def state (atom nil))

(defn create! [config]
  (let [ctx (map->Context {:config config})
        ctx (reduce (fn [ctx f]
                      (f ctx))
                    ctx
                    (:create-fns config))]
    (m/validate-humanize schema ctx)
    (reset! state ctx)))

(defn dispose! []
  (let [{:keys [ctx/assets
                ctx/batch
                ctx/cursors
                ctx/default-font
                ctx/tiled-map
                ctx/shape-drawer-texture]} @state]
    (utils/dispose! assets)
    (utils/dispose! batch)
    (run! utils/dispose! (vals cursors))
    (utils/dispose! default-font)
    (utils/dispose! shape-drawer-texture)
    (utils/dispose! tiled-map)
    ; TODO vis-ui dispose
    ; TODO what else disposable?
    ; => :ctx/tiled-map definitely and also dispose when re-creting gamestate.
    ))

(defn render! []
  (swap! state (fn [{:keys [ctx/config] :as ctx}]
                 (m/validate-humanize schema ctx)
                 (let [ctx (reduce (fn [ctx render!]
                                     (render! ctx))
                                   ctx
                                   (:render-fns config))]
                   (m/validate-humanize schema ctx)
                   ctx))))

(defn resize! [width height]
  (let [{:keys [ctx/ui-viewport
                ctx/world-viewport]} @state]
    (viewport/resize! ui-viewport    width height)
    (viewport/resize! world-viewport width height)))
