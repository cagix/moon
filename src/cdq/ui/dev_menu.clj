(ns cdq.ui.dev-menu
  (:require [cdq.ui.menu :as menu]))

(defn create
  [{:keys [ctx/graphics]
    :as ctx}
   {:keys [menus
           update-labels]}]
  (menu/create
   {:menus (for [menu menus]
             (update menu :items (fn [[f params]]
                                   (f ctx params))))
    :update-labels (for [[avar icon] update-labels]
                     (if icon
                       (avar (get (:ctx/textures graphics) icon))
                       @avar))}))
