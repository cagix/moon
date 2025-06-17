(ns cdq.utils.multifn
  (:require [cdq.effect :as effect]
            )
  (:import (clojure.lang MultiFn)))

(defn- add-methods [system-vars ns-sym k & {:keys [optional?]}]
  (doseq [system-var system-vars

          :let [method-var (resolve (symbol (str ns-sym "/" (:name (meta system-var)))))]


          ]

    (assert (or optional? method-var)
            (str "Cannot find required `" (:name (meta system-var)) "` function in " ns-sym))

    (when method-var

      (assert (keyword? k))
      (assert (var? method-var) (pr-str method-var))

      (let [system @system-var]

        (when (k (methods system))
          (println "WARNING: Overwriting method" (:name (meta method-var)) "on" k))

        ;(println "MultiFn/.addMethod " system-var  k method-var)

        (MultiFn/.addMethod system k method-var)


        ))))

(defn install-methods [{:keys [required optional]} ns-sym k]
  (require ns-sym)
  (add-methods required ns-sym k)
  (add-methods optional ns-sym k :optional? true))

; TODO
; also defmethod tx/do! can separate ....
; or draws
; grep 'defmethod' (info txt ?)

(let [effect-multis {:required [#'cdq.effect/applicable?
                                #'cdq.effect/handle]
                     :optional [#'cdq.effect/useful?
                                #'cdq.effect/render]}]
  (doseq [[ns-sym k] [
                      ['cdq.effects.audiovisual
                       :effects/audiovisual]
                      ['cdq.effects.projectile
                       :effects/projectile]
                      ['cdq.effects.sound
                       :effects/sound]
                      ['cdq.effects.spawn
                       :effects/spawn]
                      ['cdq.effects.target-all
                       :effects/target-all]
                      ['cdq.effects.target-entity
                       :effects/target-entity]
                      ['cdq.effects.target.audiovisual
                       :effects.target/audiovisual]
                      ['cdq.effects.target.convert
                       :effects.target/convert]
                      ['cdq.effects.target.damage
                       :effects.target/damage]
                      ['cdq.effects.target.kill
                       :effects.target/kill]
                      ['cdq.effects.target.melee-damage
                       :effects.target/melee-damage]
                      ['cdq.effects.target.spiderweb
                       :effects.target/spiderweb]
                      ['cdq.effects.target.stun
                       :effects.target/stun]
                      ]]
    (install-methods effect-multis ns-sym k)))



; TODO disadvantages: can't just redef defmethods !
; TODO tests for editor - keeps breaking ?


; @ cdq.ui.editor.widget.map
; L140  (comp vector? actor/user-object)
;java.lang.IllegalArgumentException: No implementation of method: :user-object of protocol: #'gdl.ui.actor/Actor found for class: gdx.ui.proxy$com.kotcrab.vis.ui.widget.VisTable$ILookup$a65747ce
; weird bug
(comment

 (gdl.ui.actor/user-object (gdx.ui/table {:user-object [:foo :bar]}))
 ; this works

 (let [ctx @cdq.application/state
       stage (:ctx/stage ctx)
       window (:property-editor-window stage)
       scroll-pane-table (group/find-actor (:scroll-pane window) "scroll-pane-table")
       m-widget-cell (first (seq (table/cells scroll-pane-table)))
       table (:map-widget scroll-pane-table)
       ]
   (clojure.pprint/pprint (seq (filter (comp vector? actor/user-object) (group/children table))))

   ;(map com.badlogic.gdx.scenes.scene2d.Actor/.getUserObject (group/children table))
   )

 ; Is it because of namespace tools ? try start w/o out ?
 ; lein clean ?
 ; ... weird !!!
 ; reproduce minimal -> understand protocols more ...

 (require [cdq.ui.editor.widget :as widget])
#_(let [multis {:optional [#'cdq.ui.editor.widget/create
                           #'cdq.ui.editor.widget/value]}]
    (doseq [[ns-sym k] [
                        ['cdq.ui.editor.widget.edn
                         :widget/edn]
                        ['cdq.ui.editor.widget.string
                         :string]
                        ['cdq.ui.editor.widget.boolean
                         :boolean]
                        ['cdq.ui.editor.widget.enum
                         :enum]
                        ['cdq.ui.editor.widget.sound
                         :s/sound]
                        ['cdq.ui.editor.widget.one-to-one
                         :s/one-to-one]
                        ['cdq.ui.editor.widget.one-to-many
                         :s/one-to-many]
                        ['cdq.ui.editor.widget.image
                         :widget/image]
                        ['cdq.ui.editor.widget.animation
                         :widget/animation]
                        ['cdq.ui.editor.widget.map
                         :s/map]
                        ]]
      (install-methods multis ns-sym k)))

 )
