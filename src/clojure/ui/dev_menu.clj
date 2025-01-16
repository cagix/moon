(ns clojure.ui.dev-menu
  (:require [clojure.graphics :as graphics]
            [clojure.db :as db]
            cdq.graphics
            [clojure.graphics.camera :as cam]
            clojure.graphics.sprite
            [clojure.scene2d.group :refer [add-actor!]]
            [clojure.ui :as ui :refer [ui-actor]]
            [clojure.utils :refer [readable-number]]
            [clojure.world :as world])
  (:import (com.badlogic.gdx.scenes.scene2d Touchable) ; clojure !!
           (com.badlogic.gdx.scenes.scene2d.ui Label Table)
           (com.kotcrab.vis.ui.widget PopupMenu)))

(defn- menu-item [text on-clicked]
  (doto (ui/menu-item text)
    (.addListener (ui/change-listener on-clicked))))

(defn- set-label-text-fn [label text-fn]
  (fn [context]
    (Label/.setText label (str (text-fn context)))))

(defn- add-upd-label
  ([c table text-fn icon]
   (let [icon (ui/image->widget (clojure.graphics.sprite/create c icon) {})
         label (ui/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (add-actor! table (ui-actor {:act (set-label-text-fn label text-fn)}))
     (.expandX (.right (Table/.add table sub-table)))))
  ([c table text-fn]
   (let [label (ui/label "")]
     (add-actor! table (ui-actor {:act (set-label-text-fn label text-fn)}))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels [c menu-bar update-labels]
  (let [table (ui/menu-bar->table menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn %))]
        (if icon
          (add-upd-label c table update-fn icon)
          (add-upd-label c table update-fn))))))

(defn- add-menu [c menu-bar {:keys [label items]}]
  (let [app-menu (ui/menu label)]
    (doseq [{:keys [label on-click]} items]
      (PopupMenu/.addItem app-menu (menu-item label (if on-click
                                                      #(on-click c)
                                                      (fn [])))))
    (ui/add-menu menu-bar app-menu)))

(defn- create-menu-bar [c menus]
  (let [menu-bar (ui/menu-bar)]
    (run! #(add-menu c menu-bar %) menus)
    menu-bar))

(defn- dev-menu* [c {:keys [menus update-labels]}]
  (let [menu-bar (create-menu-bar c menus)]
    (add-update-labels c menu-bar update-labels)
    menu-bar))

;"Mouseover-Actor: "
#_(when-let [actor (stage/mouse-on-actor? c)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn table [c config]
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

(def ^:private help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")

(defn- dev-menu-config [{:keys [clojure/db] :as c}]
  {:menus [{:label "World"
            :items (for [world (db/build-all db :properties/worlds c)]
                     {:label (str "Start " (:property/id world))
                      :on-click
                      (fn [_context])
                      ;#(world/create % (:property/id world))

                      })}
           ; TODO fixme does not work because create world uses create-into which checks key is not preseent
           ; => look at cleanup-world/reset-state/ (camera not reset - mutable state be careful ! -> create new cameras?!)
           ; => also world-change should be supported, use component systems
           {:label "Help"
            :items [{:label help-text}]}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn [{:keys [clojure.context/mouseover-eid]}]
                                 (when-let [entity (and mouseover-eid @mouseover-eid)]
                                   (:entity/id entity)))
                    :icon "images/mouseover.png"}
                   {:label "elapsed-time"
                    :update-fn (fn [{:keys [clojure.context/elapsed-time]}]
                                 (str (readable-number elapsed-time) " seconds"))
                    :icon "images/clock.png"}
                   {:label "paused?"
                    :update-fn :clojure.context/paused?} ; TODO (def paused ::paused) @ clojure.context
                   {:label "GUI"
                    :update-fn cdq.graphics/mouse-position}
                   {:label "World"
                    :update-fn #(mapv int (cdq.graphics/world-mouse-position %))}
                   {:label "Zoom"
                    :update-fn #(cam/zoom (:camera (:clojure.graphics/world-viewport %)))
                    :icon "images/zoom.png"}
                   {:label "FPS"
                    :update-fn (fn [{:keys [clojure/graphics]}]
                                 (graphics/frames-per-second graphics))
                    :icon "images/fps.png"}]})

(defn create [c]
  (table c (dev-menu-config c)))
