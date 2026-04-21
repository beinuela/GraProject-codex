import * as Sentry from '@sentry/vue'

const sentryDsn = import.meta.env.VITE_SENTRY_DSN

export const initMonitoring = (app, router) => {
  if (!sentryDsn) {
    return
  }

  Sentry.init({
    app,
    dsn: sentryDsn,
    environment: import.meta.env.VITE_SENTRY_ENVIRONMENT || import.meta.env.MODE,
    tracesSampleRate: Number(import.meta.env.VITE_SENTRY_TRACES_SAMPLE_RATE || 0)
  })

  router.onError((error) => {
    Sentry.captureException(error, {
      extra: {
        phase: 'router'
      }
    })
  })
}

export const captureFrontendError = (error, extra = {}) => {
  if (!sentryDsn) {
    return
  }
  Sentry.captureException(error, { extra })
}
