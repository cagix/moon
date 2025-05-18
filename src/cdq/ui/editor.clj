(ns cdq.ui.editor
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.property :as property]
            [cdq.ui.editor.scroll-pane :as scroll-pane]
            [cdq.ui.editor.overview-table :as overview-table]
            [cdq.ui.editor.widget :as widget]
            [cdq.ui.error-window :as error-window]
            [cdq.utils :as utils]
            [gdl.input :as input]
            [gdl.ui :as ui]))

(defn- apply-context-fn [window f]
  #(try (f)
        (ui/remove! window)
        (catch Throwable t
          (utils/pretty-pst t)
          (ui/add! ctx/stage (error-window/create t)))))

; We are working with raw property data without edn->value and build
; otherwise at update! we would have to convert again from edn->value back to edn
; for example at images/relationships
(defn editor-window [props]
  (let [schema (get ctx/schemas (property/type props))
        window (ui/window {:title (str "[SKY]Property[]")
                           :id :property-editor-window
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (widget/create schema props)
        save!   (apply-context-fn window #(do
                                           (alter-var-root #'ctx/db db/update (widget/value schema widget))
                                           (db/save! ctx/db)))
        delete! (apply-context-fn window #(do
                                           (alter-var-root #'ctx/db db/delete (:property/id props))
                                           (db/save! ctx/db)))]
    (ui/add-rows! window [[(scroll-pane/table-cell [[{:actor widget :colspan 2}]
                                                    [{:actor (ui/text-button "Save [LIGHT_GRAY](ENTER)[]" save!)
                                                      :center? true}
                                                     {:actor (ui/text-button "Delete" delete!)
                                                      :center? true}]])]])
    (.addActor window (ui/actor {:act (fn [_this _delta]
                                        (when (input/key-just-pressed? :enter)
                                          (save!)))}))
    (.pack window)
    window))

(defn- edit-property [id]
  (ui/add! ctx/stage (editor-window (db/get-raw ctx/db id))))

(defn open-editor-window! [property-type]
  (let [window (ui/window {:title "Edit"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true})]
    (ui/add! window (overview-table/create property-type edit-property))
    (.pack window)
    (ui/add! ctx/stage window)))
