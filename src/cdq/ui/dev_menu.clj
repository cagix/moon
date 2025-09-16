(ns cdq.ui.dev-menu)

(defn create
  [{:keys [ctx/graphics]
    :as ctx}
   {:keys [menus
           update-labels]}]
  {:actor/type :actor.type/table
   :rows [[{:actor {:actor/type :actor.type/menu-bar
                    :menus (for [menu menus]
                             (update menu :items (fn [[f params]]
                                                   (f ctx params))))
                    :update-labels (for [[avar icon] update-labels]
                                     (if icon
                                       (avar (get (:ctx/textures graphics) icon))
                                       @avar))}
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
