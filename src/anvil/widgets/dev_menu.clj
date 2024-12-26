(ns anvil.widgets.dev-menu
  (:require [anvil.controls :as controls]
            [anvil.widgets :as widgets]
            [cdq.context :as world]
            [clojure.gdx.graphics :as graphics]
            [gdl.app :as app]
            [gdl.context :as c]
            [gdl.graphics.camera :as cam]
            [gdl.ui :as ui :refer [ui-actor]]
            [gdl.ui.group :refer [add-actor!]])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.scenes.scene2d Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Table)))

(defn- menu-item [text on-clicked]
  (doto (ui/menu-item text)
    (.addListener (ui/change-listener on-clicked))))

(defn- add-upd-label
  ([c table text-fn icon]
   (let [icon (ui/image->widget (c/sprite c icon) {})
         label (ui/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table sub-table)))))
  ([c table text-fn]
   (let [label (ui/label "")]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels [c menu-bar update-labels]
  (let [table (ui/menu-bar->table menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn @app/state))]
        (if icon
          (add-upd-label c table update-fn icon)
          (add-upd-label c table update-fn))))))

(defn- add-menu [c menu-bar {:keys [label items]}]
  (let [app-menu (ui/menu label)]
    (doseq [{:keys [label on-click]} items]
      (.addItem app-menu (menu-item label (if on-click
                                            #(on-click c)
                                            (fn [])))))
    (ui/add-menu menu-bar app-menu)))

(defn- create-menu-bar [c menus]
  (let [menu-bar (ui/menu-bar)]
    (run! #(add-menu c menu-bar %) menus)
    menu-bar))

(defn dev-menu* [c {:keys [menus update-labels]}]
  (let [menu-bar (create-menu-bar c menus)]
    (add-update-labels c menu-bar update-labels)
    menu-bar))

;"Mouseover-Actor: "
#_(when-let [actor (stage/mouse-on-actor? c)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn dev-menu-table [c config]
  (ui/table {:rows [[{:actor (ui/menu-bar->table (dev-menu* c config))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (.setTouchable Touchable/disabled))
                      :expand? true
                      :fill-x? true
                      :fill-y? true}]]
             :fill-parent? true}))

(defn uf-dev-menu-table [c]
  (dev-menu-table c
   {:menus [{:label "Menu1"
             :items [{:label "Button1"
                      :on-click (fn [_c])}]}]
    :update-labels [{:label "GUI"
                     :update-fn c/mouse-position}
                    {:label "World"
                     :update-fn #(mapv int (c/world-mouse-position %))}
                    {:label "Zoom"
                     :update-fn #(cam/zoom (:camera (:gdl.context/world-viewport %)))
                     :icon "images/zoom.png"}
                    {:label "FPS"
                     :update-fn (fn [_c]
                                  (graphics/frames-per-second Gdx/graphics))
                     :icon "images/fps.png"}]}))

(defn- config [c]
  {:menus [{:label "World"
            :items (for [world (c/build-all c :properties/worlds)]
                     {:label (str "Start " (:property/id world))
                      :on-click #(world/create % (:property/id world))})} ; TODO fixme
           {:label "Help"
            :items [{:label controls/help-text}]}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn [c]
                                 (when-let [entity (world/mouseover-entity c)]
                                   (:entity/id entity)))
                    :icon "images/mouseover.png"}
                   {:label "elapsed-time"
                    :update-fn (fn [{:keys [cdq.context/elapsed-time]}]
                                 (str (readable-number elapsed-time) " seconds"))
                    :icon "images/clock.png"}
                   {:label "paused?"
                    :update-fn :cdq.context/paused?}
                   {:label "GUI"
                    :update-fn c/mouse-position}
                   {:label "World"
                    :update-fn #(mapv int (c/world-mouse-position %))}
                   {:label "Zoom"
                    :update-fn #(cam/zoom (:camera (:gdl.context/world-viewport %)))
                    :icon "images/zoom.png"}
                   {:label "FPS"
                    :update-fn (fn [_c]
                                 (graphics/frames-per-second Gdx/graphics))
                    :icon "images/fps.png"}]})

(defn-impl widgets/dev-menu [c]
  (dev-menu-table c (config c)))
