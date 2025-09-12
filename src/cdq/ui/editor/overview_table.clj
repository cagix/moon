(ns cdq.ui.editor.overview-table)

(defn create [image-scale rows]
  {:actor/type :actor.type/table
   :cell-defaults {:pad 5}
   :rows (for [row rows]
           (for [{:keys [texture-region
                         on-clicked
                         tooltip
                         extra-info-text]} row]
             {:actor {:actor/type :actor.type/stack
                      :actors [{:actor/type :actor.type/image-button
                                :drawable/texture-region texture-region
                                :on-clicked on-clicked
                                :drawable/scale image-scale
                                :tooltip tooltip}
                               {:actor/type :actor.type/label
                                :label/text extra-info-text
                                :actor/touchable :disabled}]}}))})
