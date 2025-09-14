(ns cdq.ui.menu
  (:require [clojure.scene2d.group :as group]
            [clojure.gdx.scene2d :as scene2d]
            [clojure.gdx.scene2d.ctx-stage :as ctx-stage]
            [clojure.scene2d.ui.table :as table]
            [clojure.gdx.scene2d.ui.label :as label]
            [clojure.gdx.scene2d.utils.listener :as listener]
            [clojure.scene2d.event :as event]
            [clojure.vis-ui.menu :as menu]
            [clojure.vis-ui.menu-bar :as menu-bar]
            [clojure.vis-ui.menu-item :as menu-item]
            [clojure.vis-ui.popup-menu :as popup-menu]
            [clojure.vis-ui.widget :as widget]))

(defn- set-label-text-actor [label text-fn]
  {:act (fn [_this _delta ctx]
          (label/set-text! label (text-fn ctx)))})

(defn- add-upd-label!
  ([table text-fn icon]
   (let [icon (widget/image icon {})
         label (widget/label {:label/text ""})
         sub-table (widget/table {:rows [[icon label]]})]
     (group/add! table (scene2d/actor (set-label-text-actor label text-fn)))
     (.expandX (.right (table/add! table sub-table)))))
  ([table text-fn]
   (let [label (widget/label {:label/text ""})]
     (group/add! table (scene2d/actor (set-label-text-actor label text-fn)))
     (.expandX (.right (table/add! table label))))))

(defn- add-update-labels! [menu-bar update-labels]
  (let [table (menu-bar/get-table menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn %))]
        (if icon
          (add-upd-label! table update-fn icon)
          (add-upd-label! table update-fn))))))

(defn- add-menu! [menu-bar {:keys [label items]}]
  (let [app-menu (menu/create label)]
    (doseq [{:keys [label on-click]} items]
      (popup-menu/add-item! app-menu (doto (menu-item/create label)
                                       (.addListener (listener/change
                                                      (fn [event actor]
                                                        (when on-click
                                                          (on-click actor (ctx-stage/get-ctx (event/stage event))))))))))
    (menu-bar/add-menu! menu-bar app-menu)))

(defn create [{:keys [menus update-labels]}]
  {:actor/type :actor.type/table
   :rows [[{:actor (let [menu-bar (menu-bar/create)]
                     (run! #(add-menu! menu-bar %) menus)
                     (add-update-labels! menu-bar update-labels)
                     (menu-bar/get-table menu-bar))
            :expand-x? true
            :fill-x? true
            :colspan 1}]
          [{:actor {:actor/type :actor.type/label
                    :label/text ""
                    :actor/touchable :disabled}
            :expand? true
            :fill-x? true
            :fill-y? true}]]
   :fill-parent? true})
