(ns cdq.ui.dev-menu
  (:require [cdq.application :as application]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.game-state :as game-state]
            [cdq.ui.editor :as editor]
            [clojure.string :as str]
            [gdl.c :as c]
            [gdl.ui.menu :as menu]
            [gdl.utils :as utils]))

(defn create [ctx]
  (menu/create
   {:menus [{:label "World"
             :items (for [world-fn (g/config ctx :world-fns)]
                      {:label (str "Start " world-fn)
                       :on-click (fn [_actor _ctx]
                                   (swap! application/state game-state/create!))})}
            {:label "Help"
             :items [{:label (g/config ctx :info)}]}
            {:label "Objects"
             :items (for [property-type (sort (g/property-types ctx))]
                      {:label (str/capitalize (name property-type))
                       :on-click (fn [_actor ctx]
                                   (editor/open-editor-window! ctx property-type))})}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                                  (when-let [entity (and mouseover-eid @mouseover-eid)]
                                    (entity/id entity)))
                     :icon (c/texture ctx "images/mouseover.png")}
                    {:label "elapsed-time"
                     :update-fn (fn [ctx]
                                  (str (utils/readable-number (g/elapsed-time ctx)) " seconds"))
                     :icon (c/texture ctx "images/clock.png")}
                    {:label "paused?"
                     :update-fn (fn [{:keys [ctx/paused?]}]
                                  paused?)}
                    {:label "GUI"
                     :update-fn (fn [ctx] (mapv int (c/ui-mouse-position ctx)))}
                    {:label "World"
                     :update-fn (fn [ctx] (mapv int (c/world-mouse-position ctx)))}
                    {:label "Zoom"
                     :update-fn c/camera-zoom
                     :icon (c/texture ctx "images/zoom.png")}
                    {:label "FPS"
                     :update-fn c/frames-per-second
                     :icon (c/texture ctx "images/fps.png")}]}))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor? ctx/stage)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))
