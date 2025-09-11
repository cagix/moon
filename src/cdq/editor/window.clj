(ns cdq.editor.window)

(defn create
  [{:keys [actors rows]}]
  {:actor/type :actor.type/window
   :title "[SKY]Property[]"
   :id :property-editor-window
   :modal? true
   :close-button? true
   :center? true
   :close-on-escape? true
   :actors actors
   :rows rows
   :cell-defaults {:pad 5}
   :pack? true})
