(ns cdq.ui.editor
  (:require [cdq.application :as application]
            [cdq.db :as db]
            [cdq.g :as g]
            [cdq.property :as property]
            [cdq.ui.editor.scroll-pane :as scroll-pane]
            [cdq.ui.editor.overview-table :as overview-table]
            [cdq.ui.editor.widget :as widget]
            [cdq.ui.error-window :as error-window]
            [cdq.utils :as utils]
            [gdl.ui :as ui]))

(defn- apply-context-fn [window f]
  (fn [{:keys [ctx/db ctx/stage]}]
    (try (f db)
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
        save!   (apply-context-fn window (fn [db]
                                           (let [new-db (db/update db (widget/value schema widget (:schemas db)))]
                                             (db/save! new-db)
                                             (swap! application/state assoc :ctx/db new-db))))
        delete! (apply-context-fn window (fn [db]
                                           (let [new-db (db/delete db (:property/id props))]
                                             (db/save! new-db)
                                             (swap! application/state assoc :ctx/db new-db))))]
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
                                        (when (g/key-just-pressed? ctx :enter)
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
