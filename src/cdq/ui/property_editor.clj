(ns cdq.ui.property-editor
  (:require [cdq.editor.scroll-pane :as scroll-pane]
            [cdq.stacktrace :as stacktrace]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.group :as group]
            [cdq.ui.table :as table]
            [cdq.ui.text-button :as text-button]
            [cdq.ui.window :as window]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn create
  [{:keys [check-enter-to-save
           delete-fn
           save?
           save-fn
           scrollpane-height
           widget
           window-opts]}]
  (let [window (window/create window-opts)
        apply-context-fn (fn [window f]
                           (fn [{:keys [ctx/stage] :as ctx}]
                             (try (f ctx)
                                  (actor/remove! window)
                                  (catch Throwable t
                                    (stacktrace/pretty-print t)
                                    (stage/add! stage (error-window/create t))))))
        save!   (apply-context-fn window save-fn)
        delete! (apply-context-fn window delete-fn)]
    (table/add-rows! window [[(scroll-pane/table-cell scrollpane-height
                                                      [[{:actor widget :colspan 2}]
                                                       [{:actor (text-button/create "Save [LIGHT_GRAY](ENTER)[]"
                                                                                    (fn [_actor ctx] (save! ctx)))
                                                         :center? true}
                                                        {:actor (text-button/create "Delete"
                                                                                    (fn [_actor ctx] (delete! ctx)))
                                                         :center? true}]])]])
    (group/add! window {:actor/type :actor.type/actor
                        :act (fn [_actor _delta {:keys [ctx/input] :as ctx}]
                               (when (save? input)
                                 (save! ctx)))})
    (.pack window)
    window))
