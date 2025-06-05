(ns cdq.ui.dev-menu
  (:require [cdq.application :as application]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.utils :as utils]
            [clojure.graphics :as graphics]
            [clojure.graphics.camera :as camera]
            [clojure.string :as str]
            [clojure.gdx.ui.menu :as menu]
            [clojure.x :as x]))

(defn create [{:keys [ctx/assets
                      ctx/config
                      ctx/db]}]
  (menu/create
   {:menus [{:label "World"
             :items (for [world-fn (:cdq.ui.dev-menu/world-fns config)]
                      {:label (str "Start " world-fn)
                       :on-click (fn [_actor _ctx]
                                   (swap! application/state ctx/reset-game-state! world-fn))})}
            {:label "Help"
             :items [{:label (:cdq.ui.dev-menu/info config)}]}
            {:label "Objects"
             :items (for [property-type (sort (db/property-types db))]
                      {:label (str/capitalize (name property-type))
                       :on-click (fn [_actor ctx]
                                   (ctx/open-editor-overview-window! ctx property-type))})}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                                  (when-let [entity (and mouseover-eid @mouseover-eid)]
                                    (entity/id entity)))
                     :icon (assets "images/mouseover.png")}
                    {:label "elapsed-time"
                     :update-fn (fn [ctx]
                                  (str (utils/readable-number (:ctx/elapsed-time ctx)) " seconds"))
                     :icon (assets "images/clock.png")}
                    {:label "paused?"
                     :update-fn (fn [{:keys [ctx/paused?]}]
                                  paused?)}
                    {:label "GUI"
                     :update-fn (fn [ctx]
                                  (mapv int (x/ui-mouse-position ctx)))}
                    {:label "World"
                     :update-fn (fn [ctx]
                                  (mapv int (x/world-mouse-position ctx)))}
                    {:label "Zoom"
                     :update-fn (comp camera/zoom :camera :ctx/world-viewport)
                     :icon (assets "images/zoom.png")}
                    {:label "FPS"
                     :update-fn (comp graphics/frames-per-second :ctx/graphics)
                     :icon (assets "images/fps.png")}]}))
