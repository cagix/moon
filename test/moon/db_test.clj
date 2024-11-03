(ns moon.db-test)


; * build quite complicated, needs a test
; * how to use db without defc systems ??
; => let components be plain namespaces ?
; => and systems call public functions ?


; (defsystem init)
; => given a 'ns'
; it just calls moon.db/init

#_(let [ns-sym 'moon.db
      system 'update!]
  (ns-resolve ns-sym system)
  )

; => components in different files
; => no defc, just public functions, no dependencies on entity/app/operation
; => easy to test, understand
; => ... possible ? maybe almost
