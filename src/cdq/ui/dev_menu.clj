(ns cdq.ui.dev-menu
  (:require [cdq.application :as application]
            [cdq.ctx.db :as db]
            [cdq.ctx.graphics :as graphics]
            [cdq.dev.data-view :as data-view]
            [cdq.editor]
            [cdq.ui.menu :as menu]
            [cdq.ui.stage :as stage]
            [cdq.utils :as utils]
            [clojure.string :as str]))

(defn mouseover-entity-id [icon]
  {:label "Mouseover-entity id"
   :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                (when-let [entity (and mouseover-eid @mouseover-eid)]
                  (:entity/id entity)))
   :icon icon})

(defn elapsed-time [icon]
  {:label "elapsed-time"
   :update-fn (fn [ctx]
                (str (utils/readable-number (:world/elapsed-time (:ctx/world ctx))) " seconds"))
   :icon icon})

(def paused
  {:label "paused?"
   :update-fn (comp :world/paused? :ctx/world)})

(def ui-mouse-position
  {:label "GUI"
   :update-fn (fn [{:keys [ctx/ui-mouse-position]}]
                (mapv int ui-mouse-position))})

(def world-mouse-position
  {:label "World"
   :update-fn (fn [{:keys [ctx/world-mouse-position]}]
                (mapv int world-mouse-position))})

(defn zoom [icon]
  {:label "Zoom"
   :update-fn (comp :camera/zoom :viewport/camera :world-viewport :ctx/graphics)
   :icon icon})

(defn fps [icon]
  {:label "FPS"
   :update-fn (comp graphics/frames-per-second :ctx/graphics)
   :icon icon})

(defn select-world [reset-game-state-fn world-fns]
  {:label "World"
   :items (for [world-fn world-fns]
            {:label (str "Start " (first world-fn))
             :on-click (fn [_actor _ctx]
                         (swap! application/state reset-game-state-fn world-fn))})})

(defn help [infotext]
  {:label "Help"
   :items [{:label infotext}]})

(defn db-editor [db]
  {:label "Objects"
   :items (for [property-type (sort (db/property-types db))]
            {:label (str/capitalize (name property-type))
             :on-click (fn [_actor ctx]
                         (cdq.editor/open-editor-overview-window! ctx property-type))})})

(def context-data-view
  {:label "Context"
   :items [{:label "Show data"
            :on-click (fn [_actor {:keys [ctx/stage] :as ctx}]
                        (stage/add! stage (data-view/table-view-window {:title "Context"
                                                                        :data ctx
                                                                        :width 500
                                                                        :height 500})))}]})

(defn- create*
  [{:keys [
           ctx/graphics
           ctx/db
           ]}
   {:keys [reset-game-state-fn
           world-fns
           info]}
   ]
  (menu/create
   {:menus [
            (select-world reset-game-state-fn world-fns)
            (help info)
            (db-editor db)
            context-data-view
            ]
    :update-labels [
                    (mouseover-entity-id (graphics/texture graphics "images/mouseover.png"))
                    (elapsed-time        (graphics/texture graphics "images/clock.png"))
                    paused
                    ui-mouse-position
                    world-mouse-position
                    (zoom (graphics/texture graphics "images/zoom.png"))
                    (fps (graphics/texture graphics "images/fps.png"))
                    ]}))

(defn create [ctx]
  (create* ctx
           {:reset-game-state-fn (requiring-resolve (:reset-game-state! (:ctx/config ctx)))
            :world-fns [['cdq.level.from-tmx/create
                         {:tmx-file "maps/vampire.tmx"
                          :start-position [32 71]}]
                        ['cdq.level.uf-caves/create
                         {:tile-size 48
                          :texture "maps/uf_terrain.png"
                          :spawn-rate 0.02
                          :scaling 3
                          :cave-size 200
                          :cave-style :wide}]
                        ['cdq.level.modules/create
                         {:world/map-size 5,
                          :world/max-area-level 3,
                          :world/spawn-rate 0.05}]]
            ;icons, etc. , components ....
            :info "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}))
