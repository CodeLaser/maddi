// Maddi.JavaCompat: slf4j's LoggerFactory for code translated from Java with the slf4j:ms-logging mapping.
//
// Java code gets its loggers from a static factory; .NET code gets them from its host's dependency injection. The
// translation keeps the static calls, and the application connects them to its host once, at startup:
//
//   JavaLogging.Factory = app.Services.GetRequiredService<ILoggerFactory>();
//
// Until then the loggers log nothing. A logger resolves the factory when it is first used, not when it is created,
// so the static logger fields of translated classes log to the factory the application set, whenever it set it.

#nullable enable

using System;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;

namespace Maddi.JavaCompat;

/// <summary>slf4j's LoggerFactory: loggers of the factory the application sets.</summary>
public static class JavaLogging
{
    public static ILoggerFactory Factory { get; set; } = NullLoggerFactory.Instance;

    // the category CreateLogger(Type) gives the type: its full name, with dots between nested types
    public static ILogger GetLogger(Type type) =>
        new Deferred(type.FullName?.Replace('+', '.') ?? type.Name, factory => factory.CreateLogger(type));

    public static ILogger GetLogger(string name) => new Deferred(name, factory => factory.CreateLogger(name));

    /// <summary>A logger of the factory as it is when the logger is first used; its name is its category.</summary>
    private sealed class Deferred(string name, Func<ILoggerFactory, ILogger> create) : ILogger
    {
        /// <summary>slf4j's Logger.getName.</summary>
        public override string ToString() => name;

        /// <summary>The logger and the factory it is of, published together: a thread sees both or neither.</summary>
        private sealed record Resolved(ILoggerFactory Factory, ILogger Logger);

        private volatile Resolved? resolved;

        private ILogger Logger
        {
            get
            {
                var factory = Factory;
                var current = resolved;
                if (current == null || !ReferenceEquals(factory, current.Factory))
                {
                    current = new Resolved(factory, create(factory));
                    resolved = current;
                }
                return current.Logger;
            }
        }

        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => Logger.BeginScope(state);

        public bool IsEnabled(LogLevel logLevel) => Logger.IsEnabled(logLevel);

        public void Log<TState>(LogLevel logLevel, EventId eventId, TState state, Exception? exception,
            Func<TState, Exception?, string> formatter) => Logger.Log(logLevel, eventId, state, exception, formatter);
    }
}
