(ns cdq.ui.dev-menu
  (:require [cdq.application :as application]
            [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.game-state :as game-state]
            [cdq.ui.editor :as editor]
            [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.ui.menu :as menu]
            [gdl.utils :as utils]
            [gdl.viewport :as viewport]))

(defn create [{:keys [ctx/assets
                      ctx/config
                      ctx/db] :as ctx}]
  (menu/create
   {:menus [{:label "World"
             :items (for [world-fn (:world-fns config)]
                      {:label (str "Start " world-fn)
                       :on-click (fn [_actor _ctx]
                                   (swap! application/state game-state/create! world-fn))})}
            {:label "Help"
             :items [{:label (:info config)}]}
            {:label "Objects"
             :items (for [property-type (sort (db/property-types db))]
                      {:label (str/capitalize (name property-type))
                       :on-click (fn [_actor ctx]
                                   (editor/open-editor-window! ctx property-type))})}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                                  (when-let [entity (and mouseover-eid @mouseover-eid)]
                                    (entity/id entity)))
                     :icon (assets/texture assets "images/mouseover.png")}
                    {:label "elapsed-time"
                     :update-fn (fn [ctx]
                                  (str (utils/readable-number (g/elapsed-time ctx)) " seconds"))
                     :icon (assets/texture assets "images/clock.png")}
                    {:label "paused?"
                     :update-fn (fn [{:keys [ctx/paused?]}]
                                  paused?)}
                    {:label "GUI"
                     :update-fn (fn [{:keys [ctx/ui-viewport]}]
                                  (mapv int (viewport/mouse-position ui-viewport)))}
                    {:label "World"
                     :update-fn (fn [{:keys [ctx/graphics]}]
                                  (mapv int (graphics/world-mouse-position graphics)))}
                    {:label "Zoom"
                     :update-fn (comp graphics/camera-zoom :ctx/graphics)
                     :icon (assets/texture assets "images/zoom.png")}
                    {:label "FPS"
                     :update-fn (comp graphics/frames-per-second :ctx/graphics)
                     :icon (assets/texture assets "images/fps.png")}]}))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor? ctx/stage)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))
