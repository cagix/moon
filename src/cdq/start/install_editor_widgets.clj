(ns cdq.start.install-editor-widgets)

(defn do! []
  (run! require '[cdq.ui.editor.widget.default
                  cdq.ui.editor.widget.edn
                  cdq.ui.editor.widget.string
                  cdq.ui.editor.widget.boolean
                  cdq.ui.editor.widget.enum
                  cdq.ui.editor.widget.sound
                  cdq.ui.editor.widget.one-to-one
                  cdq.ui.editor.widget.one-to-many
                  cdq.ui.editor.widget.image
                  cdq.ui.editor.widget.animation
                  cdq.ui.editor.widget.map]))
