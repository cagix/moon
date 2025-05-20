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
  (fn [{:keys [ctx/stage] :as ctx}]
    (try (f ctx)
         (ui/remove! window)
         (catch Throwable t
           (utils/pretty-pst t)
           (ui/add! stage (error-window/create t))))))

; We are working with raw property data without edn->value and build
; otherwise at update! we would have to convert again from edn->value back to edn
; for example at images/relationships
(defn editor-window
  [props
   {:keys [ctx/db
           ctx/ui-viewport]
    :as ctx}]
  (let [schema (get (:schemas db) (property/type props))
        window (ui/window {:title (str "[SKY]Property[]")
                           :id :property-editor-window
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (widget/create schema props ctx)
        save!   (apply-context-fn window (fn [{:keys [ctx/db]}]
                                           (alter-var-root #'ctx/db db/update (widget/value schema widget (:schemas db)))
                                           (db/save! ctx/db)))
        delete! (apply-context-fn window (fn [_ctx]
                                           (alter-var-root #'ctx/db db/delete (:property/id props))
                                           (db/save! ctx/db)))]
    (ui/add-rows! window [[(scroll-pane/table-cell (:height ui-viewport)
                                                   [[{:actor widget :colspan 2}]
                                                    [{:actor (ui/text-button "Save [LIGHT_GRAY](ENTER)[]"
                                                                             (fn [_actor ctx]
                                                                               (save! ctx)))
                                                      :center? true}
                                                     {:actor (ui/text-button "Delete"
                                                                             (fn [_actor ctx]
                                                                               (delete! ctx)))
                                                      :center? true}]])]])
    (.addActor window (ui/actor {:act (fn [_this _delta ctx]
                                        (when (input/key-just-pressed? :enter)
                                          (save! ctx)))}))
    (.pack window)
    window))

(defn- edit-property [id {:keys [ctx/stage
                                 ctx/db]
                          :as ctx}]
  (ui/add! stage (editor-window (db/get-raw db id) ctx)))

(defn open-editor-window! [{:keys [ctx/stage] :as ctx} property-type]
  (let [window (ui/window {:title "Edit"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true})]
    (ui/add! window (overview-table/create ctx property-type edit-property))
    (.pack window)
    (ui/add! stage window)))
