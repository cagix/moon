(ns clojure.ui.dev-menu
  (:require [clojure.application :as application]
            [clojure.entity :as entity]
            [clojure.ui.editor]
            [clojure.graphics.camera :as camera]
            [clojure.string :as str]
            [clojure.assets :as assets]
            [clojure.ctx :as ctx]
            [clojure.db :as db]
            [clojure.graphics :as graphics]
            [clojure.ui.menu :as menu]
            [clojure.utils :as utils]))

(defn create [{:keys [ctx/assets
                      ctx/config
                      ctx/db]}]
  (menu/create
   {:menus [{:label "World"
             :items (for [world-fn (:world-fns config)]
                      {:label (str "Start " world-fn)
                       :on-click (fn [_actor _ctx]
                                   (swap! application/state ctx/reset-game-state! world-fn))})}
            {:label "Help"
             :items [{:label (:info config)}]}
            {:label "Objects"
             :items (for [property-type (sort (db/property-types db))]
                      {:label (str/capitalize (name property-type))
                       :on-click (fn [_actor ctx]
                                   (clojure.ui.editor/open-editor-window! ctx property-type))})}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                                  (when-let [entity (and mouseover-eid @mouseover-eid)]
                                    (entity/id entity)))
                     :icon (assets/texture assets "images/mouseover.png")}
                    {:label "elapsed-time"
                     :update-fn (fn [ctx]
                                  (str (utils/readable-number (:ctx/elapsed-time ctx)) " seconds"))
                     :icon (assets/texture assets "images/clock.png")}
                    {:label "paused?"
                     :update-fn (fn [{:keys [ctx/paused?]}]
                                  paused?)}
                    {:label "GUI"
                     :update-fn (fn [ctx]
                                  (mapv int (ctx/ui-mouse-position ctx)))}
                    {:label "World"
                     :update-fn (fn [ctx]
                                  (mapv int (ctx/world-mouse-position ctx)))}
                    {:label "Zoom"
                     :update-fn (comp camera/zoom :camera :ctx/world-viewport)
                     :icon (assets/texture assets "images/zoom.png")}
                    {:label "FPS"
                     :update-fn (comp graphics/frames-per-second :ctx/graphics)
                     :icon (assets/texture assets "images/fps.png")}]}))
