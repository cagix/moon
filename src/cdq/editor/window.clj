(ns cdq.editor.window
  (:require [cdq.ui.widget :as widget]))

(defn create
  [{:keys [act-fn
           clicked-delete-fn
           clicked-save-fn
           scrollpane-height
           widget]}]
  {:actor/type :actor.type/window
   :title "[SKY]Property[]"
   :id :property-editor-window
   :modal? true
   :close-button? true
   :center? true
   :close-on-escape? true
   :rows [[(widget/scroll-pane-cell scrollpane-height
                                    [[{:actor widget :colspan 2}]
                                     [{:actor {:actor/type :actor.type/text-button
                                               :text "Save [LIGHT_GRAY](ENTER)[]"
                                               :on-clicked clicked-save-fn}
                                       :center? true}
                                      {:actor {:actor/type :actor.type/text-button
                                               :text "Delete"
                                               :on-clicked clicked-delete-fn}
                                       :center? true}]])]]
   :actors [{:actor/type :actor.type/actor
             :act act-fn}]
   :cell-defaults {:pad 5}
   :pack? true})
