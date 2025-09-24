(ns cdq.application.create.stage.dev-menu
  (:require [cdq.application.create.stage.dev-menu.ctx-data-viewer :as ctx-data-viewer]
            [cdq.application.create.stage.dev-menu.open-editor :as open-editor]
            [cdq.application.create.stage.dev-menu.help-info-text :as help-info-text]
            [cdq.application.create.stage.dev-menu.select-world :as select-world]
            [cdq.application.create.stage.dev-menu.update-labels :as update-labels]))

(defn create
  [db graphics]
  {:actor/type :actor.type/table
   :rows [[{:actor {:actor/type :actor.type/menu-bar
                    :menus [ctx-data-viewer/menu
                            (open-editor/menu db)
                            help-info-text/menu
                            select-world/menu]
                    :update-labels (for [item update-labels/items]
                                     (if (:icon item)
                                       (update item :icon #(get (:graphics/textures graphics) %))
                                       item))}
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
