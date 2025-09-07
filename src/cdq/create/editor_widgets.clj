(ns cdq.create.editor-widgets
  (:require [cdq.start]))

(defn do! [ctx]
  (cdq.start/add-methods! '[{:optional [cdq.editor.widget/create
                                        cdq.editor.widget/value]}
                            [[cdq.editor.widget.map
                              :s/map]
                             [cdq.editor.widget.default
                              :default]
                             [cdq.editor.widget.edn
                              :widget/edn]
                             [cdq.editor.widget.string
                              :string]
                             [cdq.editor.widget.boolean
                              :boolean]
                             [cdq.editor.widget.enum
                              :enum]
                             [cdq.editor.widget.sound
                              :s/sound]
                             [cdq.editor.widget.one-to-one
                              :s/one-to-one]
                             [cdq.editor.widget.one-to-many
                              :s/one-to-many]
                             [cdq.editor.widget.image
                              :widget/image]
                             [cdq.editor.widget.animation
                              :widget/animation]]]))
