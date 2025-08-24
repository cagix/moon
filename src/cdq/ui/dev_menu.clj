(ns cdq.ui.dev-menu
  (:require [cdq.application :as application]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.dev.data-view :as data-view]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [clojure.string :as str]
            [cdq.ui.menu :as menu]
            [cdq.ui.stage :as stage]
            [cdq.c :as c]
            [cdq.graphics :as graphics]
            [cdq.utils :as utils]))

(defn mouseover-entity-id [icon]
  {:label "Mouseover-entity id"
   :update-fn (fn [{:keys [ctx/world]}]
                (let [mouseover-eid (:world/mouseover-eid world)]
                  (when-let [entity (and mouseover-eid @mouseover-eid)]
                    (entity/id entity))))
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
   :update-fn (fn [ctx]
                (mapv int (c/ui-mouse-position ctx)))})

(def world-mouse-position
  {:label "World"
   :update-fn (fn [ctx]
                (mapv int (c/world-mouse-position ctx)))})

(defn zoom [icon]
  {:label "Zoom"
   :update-fn (comp :camera/zoom :viewport/camera :world-viewport :ctx/graphics)
   :icon icon})

(defn fps [icon]
  {:label "FPS"
   :update-fn (comp graphics/frames-per-second :ctx/graphics)
   :icon icon})

(defn select-world [world-fns]
  {:label "World"
   :items (for [world-fn world-fns]
            {:label (str "Start " (first world-fn))
             :on-click (fn [_actor _ctx]
                         (swap! application/state (fn [ctx]
                                                    (-> ctx
                                                        (assoc-in [:ctx/config :config/starting-world] world-fn)
                                                        ctx/reset-game-state!))))})})

(defn help [infotext]
  {:label "Help"
   :items [{:label infotext}]})

(defn db-editor [db]
  {:label "Objects"
   :items (for [property-type (sort (db/property-types db))]
            {:label (str/capitalize (name property-type))
             :on-click (fn [_actor ctx]
                         (ctx/open-editor-overview-window! ctx property-type))})})

(def context-data-view
  {:label "Context"
   :items [{:label "Show data"
            :on-click (fn [_actor {:keys [ctx/stage] :as ctx}]
                        (stage/add! stage (data-view/table-view-window {:title "Context"
                                                                        :data ctx
                                                                        :width 500
                                                                        :height 500})))}]})

(defn create
  [{:keys [
           ctx/graphics
           ctx/db
           ]}
   {:keys [world-fns
           info]}
   ]
  (menu/create
   {:menus [
            (select-world world-fns)
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
